import akka.actor.{ActorRef, Actor}

import akka.actor.Actor.Receive
import akka.pattern.ask
import scala.collection.mutable
import scala.collection.mutable.Map
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.mutable


class Server(serverSet: mutable.Set[ActorRef]) extends Actor {
  val objectsMap = mutable.Map[String, ActorRef]()
  val objects = mutable.Map[String, SharedObject]()
  val servers = serverSet
  servers.foreach(x => x ! UpdateServers(serverSet))
  if(servers.size>0) servers.head ! RequestObjects

  def searchObject(trans: Transaction, objName: String, supremum: Int): Option[ProxyObject] = {
    objectsMap.synchronized {
      if (!objectsMap.contains(objName)) {
        objectsMap += objName -> self //Where to find
        objects.synchronized {
          objects += objName -> new SharedObject(objName) //Actual value
          objects(objName).declaredUse += supremum //increase supremum
          objects(objName).storageServer = Some(self)
        }
        servers.foreach(x => x ! UpdateObjects(objectsMap)) //Inform others of new object
        Some(new ProxyObject(trans, objects(objName), supremum))
      } else if (objectsMap(objName) == self) {
        objects.synchronized {
          objects(objName).declaredUse += supremum
          objects(objName).awaiting += ((objects(objName).declaredUse - supremum) -> trans)
          val ob = objects(objName)
          Some(new ProxyObject(trans, ob, supremum))
        }
      }
      else {
        println("Object appeared at another server after query resolve")
        None
      }
    }
  }

  def checkObject(trans:Transaction, proxy: ProxyObject) = {
    if (objectsMap(proxy.innerObj.name) == self) {
      if(objects(proxy.innerObj.name).actualUse==proxy.localUsage-proxy.supremum){
        println("ALT READY SENT TO "+trans.self)
        trans.self ! Ready(objects(proxy.innerObj.name))
      }
    }
  }

  def commitObject(trans: Transaction, proxy: ProxyObject) = {
    println(self+"COMMIT MSG RECEIVED FROM "+trans.self)
    if (objectsMap(proxy.innerObj.name) == self) {
      val ob = objects(proxy.innerObj.name)
      if (ob.actualUse >= proxy.localUsage - proxy.supremum) {
        ob.value = proxy.innerObj.value
        if(ob.actualUse<proxy.localUsage){
          ob.actualUse = proxy.localUsage
        }
        if (ob.awaiting.contains(ob.actualUse)){
          println("POSTCOMMIT READY SENT TO "+trans.self)
          ob.awaiting(ob.actualUse).self ! Ready(ob)
        } //Notify next transaction in line that object is ready to use
      }
    }else if(objectsMap.contains(proxy.innerObj.name)) objectsMap(proxy.innerObj.name) ! Commit(trans,proxy)
  }

  def receive = {
    case query: Query => {
      if (!objectsMap.contains(query.objName) || objectsMap(query.objName) == self){
        val ob = searchObject(query.trans, query.objName, query.supremum)
        query.trans.self ! Success(ob)
        checkObject(query.trans,ob.get)
      }
      else if (objectsMap.contains(query.objName)) objectsMap(query.objName) ! query
    }
    case commit: Commit => {
      commitObject(commit.trans, commit.proxy)
    }
    case update: UpdateServers => {
      for (serv <- update.serverSet if (!servers.contains(serv))) servers += serv
    }
    case update: UpdateObjects => {
      for (obj <- update.objects if (!objectsMap.contains(obj._1))) objectsMap += obj
    }
  }
}
