package com.hzgc.streaming.job

import java.text.SimpleDateFormat
import java.util.Date

import com.google.gson.Gson
import com.hzgc.hbase.device.{DeviceTable, DeviceUtilImpl}
import com.hzgc.hbase.staticrepo.ObjectInfoInnerHandlerImpl
import com.hzgc.rocketmq.util.RocketMQProducer
import com.hzgc.streaming.message.OffLineAlarmMessage
import com.hzgc.streaming.util.StreamingUtils
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConverters

/**
  * 人脸识别离线告警任务（刘善彬）
  *
  */

object FaceOffLineAlarmJob {
  def main(args: Array[String]): Unit = {
    val offLineAlarmMessage = new OffLineAlarmMessage()
    val properties = StreamingUtils.getProperties
    val appName = properties.getProperty("job.offLine.appName")
    val master = properties.getProperty("job.offLine.master")
    val conf = new SparkConf().setAppName(appName).setMaster(master)
    val sc = new SparkContext(conf)
    val deviceUtilImpl = new DeviceUtilImpl()
    val offLineAlarmRule = deviceUtilImpl.getThreshold
    val separator = "ZHONGXIAN"
    if (offLineAlarmRule != null && !offLineAlarmRule.isEmpty) {
      println("Start offline alarm task data processing ...")
      val objTypeList = StreamingUtils.getOffLineArarmObjType(offLineAlarmRule)
      val returnResult = ObjectInfoInnerHandlerImpl.getInstance().searchByPkeysUpdateTime(objTypeList)
      if (returnResult != null && !returnResult.isEmpty) {
        val totalData = sc.parallelize(JavaConverters.asScalaBufferConverter(returnResult).asScala)
        val splitResult = totalData.map(totailDataElem => (totailDataElem.split(separator)(0), totailDataElem.split(separator)(1), totailDataElem.split(separator)(2)))
        val getDays = splitResult.map(splitResultElem => {
          val objRole = offLineAlarmRule.get(splitResultElem._2)
          val days = StreamingUtils.getSimilarity(objRole)
          (splitResultElem._1, splitResultElem._2, splitResultElem._3, days)
        })
        val filterResult = getDays.map(getDaysElem => (getDaysElem._1, getDaysElem._2, getDaysElem._3, StreamingUtils.timeTransition(getDaysElem._3), getDaysElem._4)).filter(f => f._4 != null).
          filter(filter => filter._4 > filter._5.toString)
        //将离线告警信息推送到MQ
        filterResult.foreach(filterResultElem => {
          val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          val dateStr = df.format(new Date());
          val rocketMQProducer = RocketMQProducer.getInstance()
          val offLineAlarmMessage = new OffLineAlarmMessage()
          val gson = new Gson()
          offLineAlarmMessage.setAlarmType(DeviceTable.OFFLINE.toString)
          offLineAlarmMessage.setStaticID(filterResultElem._1)
          offLineAlarmMessage.setUpdateTime(filterResultElem._3)
          offLineAlarmMessage.setAlarmTime(dateStr)
          val alarmStr = gson.toJson(offLineAlarmMessage)
          //离线告警信息推送的时候，平台id为对象类型字符串的前4个字节。
          val platID = filterResultElem._2.substring(0, 4)
          rocketMQProducer.send(platID, "alarm_" + DeviceTable.OFFLINE.toString, filterResultElem._1 + dateStr, alarmStr.getBytes(), null);
        })
      } else {
        println("No data was received from the static repository,the task is not running！")
      }
    } else {
      println("No object type alarm dispatched offline,the task is not running")
    }
    sc.stop()
  }


}
