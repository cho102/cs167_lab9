package edu.ucr.cs.cs167.cho102

import edu.ucr.cs.bdlab.beast.geolite.{Feature, IFeature}
import org.apache.spark.SparkConf
import org.apache.spark.beast.SparkSQLRegistration
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

import scala.collection.Map

/**
 * Scala examples for Beast
 */
object BeastScala {
  def main(args: Array[String]): Unit = {
    // Initialize Spark context

    val conf = new SparkConf().setAppName("Beast Example")
    // Set Spark master to local if not already set
    if (!conf.contains("spark.master"))
      conf.setMaster("local[*]")

    val spark: SparkSession.Builder = SparkSession.builder().config(conf)

    val sparkSession: SparkSession = spark.getOrCreate()
    val sparkContext = sparkSession.sparkContext
    SparkSQLRegistration.registerUDT
    SparkSQLRegistration.registerUDF(sparkSession)

    val operation: String = args(0)
    val inputFile: String = args(1)
    try {
      // Import Beast features
      import edu.ucr.cs.bdlab.beast._
      val t1 = System.nanoTime()
      var validOperation = true

      operation match {
        case "count-by-county" =>
        // Sample program arguments: count-by-county Tweets_1k.tsv
        // TODO count the total number of tweets for each county and display on the screen
          val tweetsDF: DataFrame = sparkSession.read.option("sep", "\t").option("header","true").csv(inputFile)
//          tweetsDF.show()
//          tweetsDF.printSchema()
          tweetsDF.selectExpr("*", "ST_CreatePoint(Longitude, Latitude) AS geometry")
          val tweetsRDD: SpatialRDD = tweetsDF.selectExpr("*", "ST_CreatePoint(Longitude, Latitude) AS geometry").toSpatialRDD
          val countiesRDD: SpatialRDD = sparkContext.shapefile("tl_2018_us_county.zip")
          val countyTweet: RDD[(IFeature, IFeature)] = countiesRDD.spatialJoin(tweetsRDD)
          val tweetsByCounty: Map[String, Long] = countyTweet
            .map({ case (county, tweet) => (county.getAs[String]("NAME"), 1) })
            .countByKey()
          println("County\tCount")
          for ((county, count) <- tweetsByCounty)
            println(s"$county\t$count")
        case "convert" =>
          val outputFile = args(2)
        // TODO add a CountyID column to the tweets, parse the text into keywords, and write back as a Parquet file
          val tweetsRDD: SpatialRDD = sparkContext.readCSVPoint(inputFile,"Longitude","Latitude",'\t')
          val countiesDF = sparkSession.read.format("shapefile").load("tl_2018_us_county.zip")
          val countiesRDD: SpatialRDD = countiesDF.toSpatialRDD
          val tweetCountyRDD: RDD[(IFeature, IFeature)] = tweetsRDD.spatialJoin(countiesRDD)
          val tweetCounty: DataFrame = tweetCountyRDD.map({ case (tweet, county) => Feature.append(tweet, county.getAs[String]("GEOID"), "CountyID") })
            .toDataFrame(sparkSession)
//          tweetCounty.printSchema()
          val convertedDF: DataFrame = tweetCounty.selectExpr("CountyID", "split(lower(text), ',') AS keywords", "Timestamp")
//          convertedDF.printSchema()
          convertedDF.write.mode(SaveMode.Overwrite).parquet(outputFile)
        case "count-by-keyword" =>
          val keyword: String = args(2)
        // TODO count the number of occurrences of each keyword per county and display on the screen
          sparkSession.read.parquet(inputFile)
            .createOrReplaceTempView("tweets")
          println("CountyID\tCount")
          sparkSession.sql(
            s"""
              SELECT CountyID, count(*) AS count
              FROM tweets
              WHERE array_contains(keywords, "$keyword")
              GROUP BY CountyID
            """).foreach(row => println(s"${row.get(0)}\t${row.get(1)}"))
        case "choropleth-map" =>
          val keyword: String = args(2)
          val outputFile: String = args(3)
        // TODO write a Shapefile that contains the count of the given keyword by county
          sparkSession.read.parquet(inputFile)
            .createOrReplaceTempView("tweets")
          sparkSession.sql(
            s"""
            SELECT CountyID, count(*) AS count
            FROM tweets
            WHERE array_contains(keywords, "$keyword")
            GROUP BY CountyID
          """).createOrReplaceTempView("keyword_counts")
          sparkContext.shapefile("tl_2018_us_county.zip")
            .toDataFrame(sparkSession)
            .createOrReplaceTempView("counties")
          sparkSession.sql(
            s"""
              SELECT CountyID, NAME, g, count
              FROM keyword_counts, counties
              WHERE CountyID = GEOID
            """)
            .toSpatialRDD
            .coalesce(1)
            .saveAsShapefile(outputFile)
        case _ => validOperation = false
      }
      val t2 = System.nanoTime()
      if (validOperation)
        println(s"Operation '$operation' on file '$inputFile' took ${(t2 - t1) * 1E-9} seconds")
      else
        Console.err.println(s"Invalid operation '$operation'")
    } finally {
      sparkSession.stop()
    }
  }
}


//
//
//import edu.ucr.cs.bdlab.beast.cg.SpatialJoinAlgorithms.{ESJDistributedAlgorithm, ESJPredicate}
//import edu.ucr.cs.bdlab.beast.geolite.{Feature, IFeature}
//import org.apache.spark.SparkConf
//import org.apache.spark.beast.{CRSServer, SparkSQLRegistration}
//import org.apache.spark.rdd.RDD
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.sql.types.{StringType, StructField, StructType}
//import org.locationtech.jts.geom.{Envelope, GeometryFactory}
//
///**
// * Scala examples for Beast
// */
//object BeastScala {
//  def main(args: Array[String]): Unit = {
//    // Initialize Spark context
//
//    val conf = new SparkConf().setAppName("Beast Example")
//    // Set Spark master to local if not already set
//    if (!conf.contains("spark.master"))
//      conf.setMaster("local[*]")
//
//    // Start the CRSServer and store the information in SparkConf
//    CRSServer.startServer(conf)
//    val sparkSession: SparkSession = SparkSession.builder().config(conf).getOrCreate()
//    val sparkContext = sparkSession.sparkContext
//    CRSServer.stopOnExit(sparkContext)
//    SparkSQLRegistration.registerUDT
//    SparkSQLRegistration.registerUDF(sparkSession)
//
//    try {
//      // Import Beast features
//      import edu.ucr.cs.bdlab.beast._
//
//      // Load spatial datasets
//      // Load a shapefile.
//      // Download from: ftp://ftp2.census.gov/geo/tiger/TIGER2018/STATE/
//      val polygons = sparkContext.shapefile("tl_2018_us_state.zip")
//      // Load points in GeoJSON format.
//      // Download from https://star.cs.ucr.edu/dynamic/download.cgi/Tweets/index.geojson?mbr=-117.8538,33.2563,-116.8142,34.4099&point
//      val points = sparkContext.geojsonFile("Tweets_index.geojson")
//
//      // Run a range query
//      val geometryFactory: GeometryFactory = new GeometryFactory()
//      val range = geometryFactory.toGeometry(new Envelope(-117.337182, -117.241395, 33.622048, 33.72865))
//      val matchedPolygons: RDD[IFeature] = polygons.rangeQuery(range)
//      val matchedPoints: RDD[IFeature] = points.rangeQuery(range)
//
//      // Run a spatial join operation between points and polygons (point-in-polygon) query
//      val sjResults: RDD[(IFeature, IFeature)] =
//        matchedPolygons.spatialJoin(matchedPoints, ESJPredicate.Contains, ESJDistributedAlgorithm.PBSM)
//
//      // Keep point coordinate, text, and state name
//      val finalResults: RDD[IFeature] = sjResults.map(pip => {
//        val polygon: IFeature = pip._1
//        val point: IFeature = pip._2
//        val values = point.toSeq :+ polygon.getAs[String]("NAME")
//        val schema = StructType(point.schema :+ StructField("state", StringType))
//        new Feature(values.toArray, schema)
//      })
//
//      // Write the output in CSV format
//      finalResults.saveAsCSVPoints(filename = "output", xColumn = 0, yColumn = 1, delimiter = ';')
//    } finally {
//      sparkSession.stop()
//      CRSServer.stopServer(false)
//    }
//  }
//}
