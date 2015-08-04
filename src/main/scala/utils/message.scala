import akka.actor.ActorRef

import scala.collection.mutable

//Message types

case class Query(trans:Transaction,objName:String,supremum:Int)
case class Success(proxy:Option[ProxyObject])
case class Ready(obj:SharedObject)
case class Commit(trans:Transaction,proxy:ProxyObject)
case class UpdateServers(serverSet:mutable.Set[ActorRef])
case class UpdateObjects(objects:mutable.Map[String,ActorRef])
case class UpdateProxy(proxy:ProxyObject)
case class Access(objName:String,supremum:Int)
case class AccessReply(pack:PackagedObject)
case class Claim()
case class RequestObjects()
case class CommitTransaction()