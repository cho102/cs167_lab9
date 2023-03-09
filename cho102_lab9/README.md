# Lab 9

## Student information

* Full name: Cindy Ho 
* E-mail: cho102@ucr.edu
* UCR NetID: cho102
* Student ID: 862151318

## Answers

* (Q1) What is the schema of the file after loading it as a Dataframe

    ```text
    # Replace here with the schema as printed by Spark
    root
     |-- Timestamp: string (nullable = true)
     |-- Text: string (nullable = true)
     |-- Latitude: string (nullable = true)
     |-- Longitude: string (nullable = true)
    ```

* (Q2) Why in the second operation, convert, the order of the objects in the  tweetCounty RDD is (tweet, county) while in the first operation, count-by-county, the order of the objects in the spatial join result was (county, tweet)?

    ```text
    # Replace with your answer
    In count-by-county, countiesRDD performed the spatial join with tweetsRDD as its parameter [val countyTweet: RDD[(IFeature, IFeature)] = countiesRDD.spatialJoin(tweetsRDD)] which is why the result was (county,tweet), whereas in the second operation, tweetsRDD perform the join operation with countiesRDD as the parameter[val tweetCountyRDD: RDD[(IFeature, IFeature)] = tweetsRDD.spatialJoin(countiesRDD)]. 
    ```

* (Q3) What is the schema of the tweetCounty Dataframe?

    ```text
    # Replace here with the schema as printed by Spark
    root
      |-- g: geometry (nullable = true)
      |-- Timestamp: string (nullable = true)
      |-- Text: string (nullable = true)
      |-- CountyID: string (nullable = true)
    ```

* (Q4) What is the schema of the convertedDF Dataframe?

    ```text
    # Replace here with the schema as printed by Spark
    root
  |-- CountyID: string (nullable = true)
  |-- keywords: array (nullable = true)
  |    |-- element: string (containsNull = true)
  |-- Timestamp: string (nullable = true)
    ```

* (Q5) For the tweets_10k dataset, what is the size of the decompressed ZIP file as compared to the converted Parquet file?

    | Size of the original decompressed file | Size of the Parquet file |
    | - | - |
    |  788.91kB | 349.659kB |

* (Q6) (Bonus) Write down the SQL query(ies) that you can use to compute the ratios as described above. Briefly explain how your proposed solution works.

    ```SQL
    -- Enter the SQL query(ies) here
    ```

    ```text
    Use this space to explain how it works.
    ```
