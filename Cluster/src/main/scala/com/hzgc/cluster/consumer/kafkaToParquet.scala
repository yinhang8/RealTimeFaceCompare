package com.hzgc.cluster.consumer

import java.sql.Timestamp
import java.util.Properties

import com.hzgc.cluster.util.StreamingUtils
import com.hzgc.ftpserver.producer.{FaceObject, FaceObjectDecoder}
import com.hzgc.hbase.util.FtpUtil
import kafka.serializer.StringDecoder
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Duration, Durations, StreamingContext}

object kafkaToParquet {

  def getItem(parameter: String, properties: Properties): String = {
    val item = properties.getProperty(parameter)
    if (null != item) {
      return item
    } else {
      println("Please check the parameter " + parameter + " is correct!!!")
      System.exit(1)
    }
    null
  }

  case class Picture(ftpurl: String, //图片搜索地址
                     feature: Array[Float], ipcid: String, timeslot: Int, //feature：图片特征值 ipcid：设备id  timeslot：时间段
                     exacttime: Timestamp, searchtype: String, date: String, //timestamp:时间戳 pictype：图片类型 date：时间
                     eyeglasses: Int, gender: Int, haircolor: Int, //人脸属性：眼镜、性别、头发颜色
                     hairstyle: Int, hat: Int, huzi: Int, tie: Int //人脸属性：发型、帽子、胡子、领带
                    )

  val properties: Properties = StreamingUtils.getProperties
  val appname: String = getItem("job.faceObjectConsumer.appName", properties)
  val master: String = getItem("job.faceObjectConsumer.master", properties)
  val timeInterval: Duration = Durations.seconds(getItem("job.faceObjectConsumer.timeInterval", properties).toLong)
  val brokers: String = getItem("job.faceObjectConsumer.broker.list", properties)
  val kafkaGroupId: String = getItem("job.faceObjectConsumer.group.id", properties)
  val topics = Set(getItem("job.faceObjectConsumer.topic.name", properties))
  val repartitionNum: Int = getItem("job.repartition.number", properties).toInt
  val storeAddress: String = getItem("job.storeAddress", properties)
  val backupAddress: String = getItem("job.backupAddress", properties)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().appName(appname).master(master).getOrCreate()
    val ssc = new StreamingContext(spark.sparkContext, timeInterval)
    spark.sparkContext.setCheckpointDir(backupAddress)
    val kafkaParams = Map(
      "metadata.broker.list" -> brokers,
      "group.id" -> kafkaGroupId
    )
    val putDataToEs = PutDataToEs.getInstance()
    val kafkaDstream = KafkaUtils.createDirectStream[String, FaceObject, StringDecoder, FaceObjectDecoder](ssc, kafkaParams, topics)
    val kafkaDF = kafkaDstream.map(faceobject => {
      val status = putDataToEs.putDataToEs(faceobject._1, faceobject._2)
      if (status != 1) {
        println("Put data to es failed!")
      }
      Picture(FtpUtil.getFtpUrl(faceobject._1), faceobject._2.getAttribute.getFeature, faceobject._2.getIpcId,
        faceobject._2.getTimeSlot.toInt, Timestamp.valueOf(faceobject._2.getTimeStamp), faceobject._2.getType.name(),
        faceobject._2.getDate, faceobject._2.getAttribute.getEyeglasses, faceobject._2.getAttribute.getGender,
        faceobject._2.getAttribute.getHairColor, faceobject._2.getAttribute.getHairStyle, faceobject._2.getAttribute.getHat,
        faceobject._2.getAttribute.getHuzi, faceobject._2.getAttribute.getTie
      )
    })
    kafkaDF.foreachRDD(rdd => {
      rdd.foreachPartition(par => {
        par.foreach(println)
      })
      import spark.implicits._
      rdd.coalesce(1).toDF().cache().checkpoint().write.mode(SaveMode.Append).parquet(storeAddress)
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
