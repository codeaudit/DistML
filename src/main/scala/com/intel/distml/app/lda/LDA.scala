package com.intel.distml.app.lda

import com.intel.distml.api.{BigModelWriter, Model}
import com.intel.distml.model.lda.{LDAModel, LDADataMatrix, Dictionary}
import com.intel.distml.platform.{TrainingContext, TrainingHelper}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._

import scala.collection.mutable.ListBuffer

/**
 * Created by ruixiang on 6/26/15.
 */
object LDA {

  var testDoc = "alt.atheism     re who has read rushdie s the satanic verses in article vice ico tek com bobbe vice ico tek com robert beauchaine wrote in article edm apr gocart twisto compaq com edm twisto compaq com ed mccreary writes while we re on the topic of books has anyone else noticed that paine s the age of reason is hard to find i ve been wanting to pick up a copy for a while but not bad enough to mail order it i ve noticed though that none of the bookstores i go to seem to carry it i thought this was supposed to be classic what s the deal me too our local used book store is the second largest on the west coast and i couldn t find a copy there i guess atheists hold their bibles in as much esteem as the theists if i remember correctly prometheus books have this one in stock so just call them and ask for the book cheers kent sandvik newton apple com alink ksand private activities on the net"

  def normalizeString(src : String) : String = {
    src.replaceAll("[^A-Z^a-z]", " ").trim().toLowerCase();
  }

  var dic = new Dictionary()
  val K = 20000   //topic number

  def main(args: Array[String]) {

    var sparkMaster = args(0)
    var sparkHome = args(1)
    var sparkMem = args(2)
    var appJars = args(3)

    println("envir Info: spark master:"+sparkMaster+" spark home:"+sparkHome+" spark memo:"+sparkMem+" app jar:"+appJars)

    val conf = new SparkConf()
      .setMaster(sparkMaster)
      .setAppName("LDA")
      .set("spark.executor.memory", sparkMem)
      .set("spark.home", sparkHome)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer.mb", "500")
      .setJars(Seq(appJars))

    val spark = new SparkContext(conf)
    Thread.sleep(3000)

    println("====================start:  ==========")
//    val trainingFile: String = "hdfs://dl-s1:9000/usr/ruixiang/lda/newdocs2.dat"
    val trainingFile: String = "hdfs://dl-s1:9000/data/wiki/wiki_1000000"
    //val trainingFile: String = "hdfs://dl-s1:9000/data/text/20ng-train-all-terms.txt"

    var rawLines = spark.textFile(trainingFile).map(normalizeString).filter(s => s.length > 0)
//    rawLines = rawLines.repartition(2);
    println("====================the training file's lines number: " + rawLines.count() + " ==========")

    val words = rawLines.flatMap(line => line.split(" ")).distinct().collect()
    words.foreach(x=>dic.addWord(x))

    println("====================the word number: " + words.size + " ==========")

    var rddTopic = rawLines.mapPartitions(transFromString2LDAData)
    //rddTopic=rddTopic.repartition(1);

    val config: TrainingContext = new TrainingContext();
    config.iteration(1);
    config.miniBatchSize(20);
    config.psCount(4);
//    config.workerCount(1)

    val m: Model = new LDAModel(0.5f, 0.1f, K, dic.getSize)

    val writer = new BigModelWriter();
    writer.setParamRowSize(LDAModel.MATRIX_PARAM_WORDTOPIC, K * 10);
    TrainingHelper.startTraining(spark, m, rddTopic, config, writer);
    System.out.println("LDA has ended!")

    spark.stop()
  }


  def transFromString2LDAData(itr:Iterator[String]) = {
    var lst = ListBuffer[LDADataMatrix]();

    while(itr.hasNext) {
      val line = itr.next();
      val words = line.split(" ")

      var wordIDs = new ListBuffer[Int]();
      words.foreach(x => wordIDs += dic.addWord(x))

      val topics = new ListBuffer[Int]()
      wordIDs.foreach(x => {
        var topic = Math.floor(Math.random()*K).toInt
        if (topic >= 20000) {
          System.out.println("invalid topic: " + topic)
          System.exit(1)
        }
        topics += topic
      })

      val doctopic = new Array[Int](K)
      topics.foreach(x => doctopic(x) = doctopic(x) + 1)

      lst += new LDADataMatrix(topics.toArray, wordIDs.toArray, doctopic)

    }
    lst.iterator
  }

}
