package songs

import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.{SparkConf, SparkContext, mllib}
import org.apache.spark.mllib.regression.LinearRegressionModel
import org.apache.spark.sql.SQLContext
import org.slf4j.LoggerFactory

object EvaluateModel {

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName(Config.appName)
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val logger = LoggerFactory.getLogger(getClass.getName)

    logger.info(s"Loading Linear Regression Model from ${Config.modelOut}")
    val model = SongML.loadLinearRegressionModel(sc, Config.modelOut)

    logger.info("Printing weights and intercept for Linear Regression Model")
    val colWeights = SongML.featureColumns.zip(model.weights.toArray)
    logger.info(s"Weights: $colWeights")
    logger.info(s"Intercept: ${model.intercept}")

    logger.info("Loading datasets")
    val datasets = SongML.loadModelData(sqlContext = sqlContext)
    val pipelineModel = SongML.transformPipeline.fit(datasets.test)
    val testData = pipelineModel.transform(datasets.test)

    logger.info("Calculating Regression Metrics")
    val testFeatures = testData.select(SongML.featuresColumn).map(r => r.getAs[mllib.linalg.Vector](SongML.featuresColumn))
    val testPredictions = testFeatures.map(model.predict)
    val testLabels = testData.select(SongML.labelColumn).map(r => r.getAs[Double](SongML.labelColumn))

    val rm = new RegressionMetrics(testPredictions.zip(testLabels).map(t => (t._1, t._2)))

    logger.info("Test Metrics")
    logger.info("Test Explained Variance:")
    logger.info(s"${rm.explainedVariance}")
    logger.info("Test R^2:")
    logger.info(s"${rm.r2}")
    logger.info("Test MSE:")
    logger.info(s"${rm.meanSquaredError}")
    logger.info("Test RMSE:")
    logger.info(s"${rm.rootMeanSquaredError}")

    logger.info("Exiting")
    sc.stop()
  }

}
