import akka.actor.{ActorRef, Actor}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Transaction (entry:ActorRef) extends Actor{
  val objMap = mutable.Map[ProxyObject,Boolean]()
  val proxyMap = mutable.Map[String,ProxyObject]()
  val entryPoint =entry
  var client:ActorRef = null

  def access(objName:String,supremum:Int):PackagedObject = {
    //Query server about object
    entryPoint ! Query(this,objName,supremum)
    val tempObj = new SharedObject(objName)
    val ob = new ProxyObject(this,tempObj,supremum)
    objMap+=ob->false
    proxyMap+=objName->ob
    new PackagedObject(ob)
  }

  def commit(proxy:ProxyObject) = {
    //Commit an object, allowing others to use it
    entryPoint ! Commit(this,proxy)
    objMap-=proxy
    proxyMap-=proxy.innerObj.name
  }

  def commitTransaction = {
    for(ob <- proxyMap.values) commit(ob)
  }

  def receive = {
      case msg:Ready => {
        if(proxyMap.contains(msg.obj.name)){
          proxyMap(msg.obj.name).innerObj=msg.obj
          objMap(proxyMap(msg.obj.name))=true
          client ! Ready(msg.obj)
        }
        else println("This was not the object I was looking for. "+msg.obj)
      }
      case msg:Success => {
        //New proxy acquired
        if(msg.proxy.nonEmpty){
          proxyMap(msg.proxy.get.innerObj.name)=msg.proxy.get
          client ! UpdateProxy(msg.proxy.get)
        }
      }

      case msg:Claim => {
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
