package org.apache.spark.angel.ml.classification

import com.tencent.angel.sona.core.DriverContext

import org.apache.spark.angel.ml.feature.LabeledPoint
import org.apache.spark.angel.ml.linalg.Vectors
import org.apache.spark.{SparkConf, SparkFunSuite}
import org.apache.spark.sql.{DataFrameReader, SparkSession}



class AngelClassificationSuite extends SparkFunSuite {
  private var spark: SparkSession = _
  private var libsvm: DataFrameReader = _
  private var dummy: DataFrameReader = _
  private var sparkConf: SparkConf = _
  private var driverCtx: DriverContext = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder()
      .master("local[2]")
      .appName("AngelClassification")
      .getOrCreate()

    libsvm = spark.read.format("libsvmex")
    dummy = spark.read.format("dummy")
    sparkConf = spark.sparkContext.getConf
    driverCtx = DriverContext.get(sparkConf)

    driverCtx.startAngelAndPSAgent()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    driverCtx.stopAngelAndPSAgent()
  }

  test("daw_train") {
    spark = SparkSession.builder()
      .master("local[2]")
      .appName("AngelClassification")
      .getOrCreate()

    libsvm = spark.read.format("libsvmex")
    dummy = spark.read.format("dummy")

    val trainData = libsvm.load("./data/angel/census/census_148d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/daw.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/daw")
  }

  test("daw_predict") {
    val trainData = libsvm.load("./data/angel/census/census_148d_train.libsvm")

    val predictor = AngelClassifierModel.read.load("trained_models/daw")

    val res = predictor.transform(trainData)
    res.show()
    res.write.mode("overwrite").save("predict_results/daw")
  }

  test("svm_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = dummy.load("./data/angel/a9a/a9a_123d_train.dummy")

    val svm_classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/svm.json")
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(14)

    val svm_model = svm_classifier.fit(trainData)

    svm_model.write.overwrite().save("trained_models/svm")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("svm_predict") {
    val trainData = dummy.load("./data/angel/a9a/a9a_123d_train.dummy")

    val predictor = AngelClassifierModel.read.load("trained_models/svm")

    val res = predictor.transform(trainData)
    res.show()
    res.write.mode("overwrite").save("predict_results/svm")
  }

  test("softmax_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = libsvm.load("./data/angel/protein/protein_357d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/softmax.json")
      .setNumClass(3)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(22)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/softmax")
//    driverCtx.stopAngelAndPSAgent()
  }

  test("robustreg_train") {//todo???
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = libsvm.load("./data/angel/abalone/abalone_8d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/robustreg.json")
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(14)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/robustreg")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("kmeans_train") {//todo???
  //    val driverCtx = DriverContext.get(sparkConf)
  //    driverCtx.startAngelAndPSAgent()
  val trainData = libsvm.load("./data/angel/usps/usps_256d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/kmeans.json")
      .setNumClass(10)//todo???
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/kmeans")

    //    driverCtx.stopAngelAndPSAgent()
  }

  test("mixedlr_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = libsvm.load("data/angel/a9a/a9a_123d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/mixedlr.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(14)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/mixedlr")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("logreg_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = libsvm.load("data/angel/a9a/a9a_123d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/linreg.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(14)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/logreg")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("linreg_train") {//todo???
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = libsvm.load("./data/angel/abalone/abalone_8d_train.libsvm")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/linreg.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(14)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/linreg")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("dnn_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
//    val trainData = libsvm.load("data/angel/census/census_148d_train.libsvm")

    val Data = spark.sparkContext.textFile("./data/angel/census/census_148d_train.dense").map(_.split(" "))
    val Data_ = Data.map(x => LabeledPoint(x(0).toDouble, Vectors.dense(x.slice(1, x.length-1).map(_.toDouble))))
    val trainData = spark.createDataFrame(Data_)

    trainData.show()

    val dnn_classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/dnn.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    println(dnn_classifier.getNumClass)
    val model = dnn_classifier.fit(trainData)

    model.write.overwrite().save("trained_models/dnn")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("deepfm_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = dummy.load("data/angel/census/census_148d_train.dummy")

    val deepfm_classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/deepfm.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    val model = deepfm_classifier.fit(trainData)

    model.write.overwrite().save("trained_models/deepfm")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("fm_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = dummy.load("data/angel/census/census_148d_train.dummy")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/deepfm.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/fm")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("nfm_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = dummy.load("data/angel/census/census_148d_train.dummy")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/deepfm.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/nfm")

//    driverCtx.stopAngelAndPSAgent()
  }

  test("pnn_train") {
//    val driverCtx = DriverContext.get(sparkConf)
//    driverCtx.startAngelAndPSAgent()
    val trainData = dummy.load("data/angel/census/census_148d_train.dummy")

    val classifier = new AngelClassifier()
      .setModelJsonFile("./angelml/src/test/jsons/deepfm.json")
      .setNumClass(2)
      .setNumBatch(10)
      .setMaxIter(2)
      .setLearningRate(0.1)
      .setNumField(13)

    val model = classifier.fit(trainData)

    model.write.overwrite().save("trained_models/pnn")

//    driverCtx.stopAngelAndPSAgent()
  }
}
