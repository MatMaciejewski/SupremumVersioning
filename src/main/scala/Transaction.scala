import akka.actor.{ActorRef, Actor}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by prophet on 07.02.15.
 */
class Transaction (entry:ActorRef) extends Actor{
  val objMap = mutable.Map[ProxyObject,Boolean]()
  val proxyMap = mutable.Map[String,ProxyObject]()
  val entryPoint =entry
  var client:ActorRef = null

  def access(objName:String,supremum:Int):PackagedObject = {
    entryPoint ! Query(this,objName,supremum)
    val tempObj = new SharedObject(objName)
    val ob = new ProxyObject(this,tempObj,supremum)
    objMap+=ob->false
    proxyMap+=objName->ob
    new PackagedObject(ob)
  }

  def commit(proxy:ProxyObject) = {
    entryPoint ! Commit(this,proxy)
    objMap-=proxy
    proxyMap-=proxy.innerObj.name
  }

  def commitTransaction = {
    for(ob <- proxyMap.values) commit(ob)
  }

  def receive = {
      case msg:Ready => {
//        println(self+" HERE'S THE CULPRIT. BEFORE: "+proxyMap(msg.obj.name).localUsage+" AND AFTER: "+msg.obj.declaredUse)
        if(proxyMap.contains(msg.obj.name)){
          proxyMap(msg.obj.name).innerObj=msg.obj
          objMap(proxyMap(msg.obj.name))=true
          client ! Ready(msg.obj)
        }
        else println("This was not the object I was looking for. "+msg.obj)
//        println(self+" AFTER: "+proxyMap(msg.obj.name).localUsage)
      }
      case msg:Success => {
//        println(self+": NEW PROXY ACQUIRED. "+msg.proxy.get.localUsage)
        if(msg.proxy.nonEmpty){
//          println(self+": STILL "+msg.proxy.get.localUsage)
          proxyMap(msg.proxy.get.innerObj.name)=msg.proxy.get
          client ! UpdateProxy(msg.proxy.get)
//          println(self+": AFTER "+msg.proxy.get.localUsage)
        }
//        else Query()
      }

      case msg:Claim => {
//        println(self+": Transaction claimed by "+sender)
        if(client==null)client=sender
        else println("Attempt to claim transaction for the second time.")
      }

      case msg:Commit => {
        commit(msg.proxy)
      }

      case msg:CommitTransaction => {
        commitTransaction
      }

      case acc:Access => {
        sender ! AccessReply(access(acc.objName,acc.supremum))
      }
    }
}
