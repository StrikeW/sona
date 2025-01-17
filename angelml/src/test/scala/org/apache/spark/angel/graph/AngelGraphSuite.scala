
package org.apache.spark.angel.graph


import org.apache.spark.angel.graph.line.LINE
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import com.tencent.angel.sona.core.DriverContext
import javassist.bytecode.SignatureAttribute.ArrayType
import org.apache.spark.angel.graph.kcore.KCore
import org.apache.spark.angel.graph.line.LINE
import org.apache.spark.angel.graph.louvain.Louvain
import org.apache.spark.angel.graph.utils.GraphIO
import org.apache.spark.angel.graph.word2vec.{Word2Vec, Word2VecModel}
import org.apache.spark.angel.ml.feature.LabeledPoint
import org.apache.spark.angel.ml.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{DataType, IntegerType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, DataFrameReader, Row, SparkSession}

import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext, SparkFunSuite}

import scala.util.Random


class AngelGraphSuite extends SparkFunSuite {
  private var spark: SparkSession = _
  private var sparkConf: SparkConf = _
  private var sc: SparkContext = _
  private val numPartition: Int = 2
  private val storageLevel: StorageLevel = StorageLevel.MEMORY_ONLY
  private var numEdge: Long = -1
  private var maxNodeId: Long = -1

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder()
      .master(s"local[$numPartition]")
      .appName("AngelGraph")
      .getOrCreate()

    sc = spark.sparkContext
    sparkConf = spark.sparkContext.getConf
  }

  override def afterAll(): Unit = {
    super.afterAll()
    spark.close()
  }

  def readData(input: String, sep: String = " "): DataFrame = {
    val rdd = sc.textFile(input).mapPartitions { iter =>
      val r = new Random()
      iter.map { line =>
        val arr = line.split(sep)
        val src = arr(0).toInt
        val dst = arr(1).toInt
        (r.nextInt, Row(src, dst))
      }
    }.repartition(numPartition).values.persist(storageLevel)

    numEdge = rdd.count()
    maxNodeId = rdd.map { case Row(src: Int, dst: Int) => math.max(src, dst) }.max().toLong + 1

    val schema = StructType(Array(StructField("src", IntegerType), StructField("dst", IntegerType)))
    val data = spark.createDataFrame(rdd, schema)

    println(s"numEdge=$numEdge maxNodeId=$maxNodeId")

    data
  }

  test("readData") {
    val input = "../data/angel/bc/edge"
    val data = readData(input)
    data.printSchema()
    data.show(10)
  }

  test("line: default params ") {
    val line = new LINE()
    assert(line.getNumEpoch === 10)
    assert(line.getStepSize === 0.00001)
    assert(line.getSrcNodeIdCol === "src")
    assert(line.getDstNodeIdCol === "dst")
  }

  test("line1") {
    val input = "../data/angel/bc/edge"
    val data = readData(input)
    data.printSchema()
    data.show(10)

    val line = new LINE()
      .setStepSize(0.025)
      .setEmbeddingDim(32)
      .setBatchSize(1024)
      .setNumPSPart(2)
      .setNumEpoch(2)
      .setNegSample(5)
      .setOrder(2)
      .setMaxIndex(maxNodeId.toInt)
      .setSrcNodeIdCol("src")
      .setDstNodeIdCol("dst")

    val model = line.fit(data)

    line.write.overwrite().save("trained_models/lineAlgo")

    //todo???
//    model.write.overwrite().save("trained_models/lineModels")
  }

  test("kcore") {
    val input = "./data/angel/bc/edge"
    val data = GraphIO.load(input, isWeighted = false, 0, 1, sep = " ")
    data.printSchema()
    data.show(10)

    val kCore = new KCore()
      .setPartitionNum(100)
      .setStorageLevel(storageLevel)
      .setPSPartitionNum(10)
      .setSrcNodeIdCol("src")
      .setDstNodeIdCol("dst")

    val mapping = kCore.transform(data)
    GraphIO.save(mapping, "trained_models/kCoreAlgo")
  }
  
  
  test("kcore1") {
    val input = "./data/angel/bc/edge"
    val output = "trained_models/kcore1/edge"
    val partitionNum = 3
    val storageLevel = StorageLevel.MEMORY_ONLY
    val psPartitionNum = 2

    val df = GraphIO.load(input, isWeighted = false)

    val kcore = new KCore()
      .setPartitionNum(partitionNum)
      .setStorageLevel(storageLevel)
      .setPSPartitionNum(psPartitionNum)

    val mapping = kcore.transform(df)
    GraphIO.save(mapping, output)
  }

  test("louvain") {
    val input = "./data/angel/bc/edge"
    val data = GraphIO.load(input, isWeighted = false, 0, 1, sep = " ")
    data.printSchema()
    data.show(10)
    sc.setCheckpointDir("trained_models/louvainAlgo/cp")

    val louvain = new Louvain()
      .setPartitionNum(4)
      .setStorageLevel(storageLevel)
      .setNumFold(2)
      .setNumOpt(5)
      .setBatchSize(100)
      .setDebugMode(true)
      .setEps(0.0)
      .setBufferSize(100000)
      .setIsWeighted(false)
      .setPSPartitionNum(2)
      .setSrcNodeIdCol("src")
      .setDstNodeIdCol("dst")

    val mapping = louvain.transform(data)
    GraphIO.save(mapping, "trained_models/louvainAlgo")
  }

//  test("word2vec") {
//    val input = "./data/angel/text8/text8.split.head"
//    val data = sc.textFile(input)
//    data.cache()
//
//    val (corpus) = corpusStringToIntWithoutRemapping(sc.textFile(input))
//    val docs = corpus.repartition(1)
////    val schema = StructType(Array(StructField("src", ArrayType[Int])))
////    val df = spark.createDataFrame(docs.map{case arr:Array[Int] => Row(arr)}, schema)
//
//    docs.cache()
//    docs.count()
//    data.unpersist()
//
//    val numDocs = docs.count()
//    val maxWordId = docs.map(_.max).max() + 1
//    val numTokens = docs.map(_.length).sum()
//    val maxLength = docs.map(_.length).max()
//
//    println(s"numDocs=$numDocs maxWordId=$maxWordId numTokens=$numTokens")
//
//    val word2vec = new Word2Vec()
//      .setEmbeddingDim(10)
//      .setBatchSize(100)
//      .setModel("cbow")
//      .setNegSample(5)
//      .setMaxIndex(maxWordId)
//      .setMaxLength(maxLength)
//      .setCheckpointInterval(1000)
//      .setNumEpoch(5)
//      .setNumPSPart(1)
//      .setPartitionNum(5)
//      .setStepSize(1.0)
//      .setWindowSize(5)
////    val model = word2vec.fit(df)
//  }

  def corpusStringToIntWithoutRemapping(data: RDD[String]): RDD[Array[Int]] = {
    data.filter(f => f != null && f.length > 0)
      .map(f => f.stripLineEnd.split("[\\s+|,]").map(s => s.toInt))
  }
}
