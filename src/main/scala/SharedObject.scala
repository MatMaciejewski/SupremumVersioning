import akka.actor.ActorRef

import scala.collection.mutable

/**
 * Created by prophet on 07.02.15.
 */
class SharedObject(objectName:String) {
  val name = objectName
  val awaiting = mutable.Map[Int,Transaction]()       //localUsage from proxy - supremum -> Transaction
  var value=0.0
  var declaredUse=0
  var actualUse=0
  var storageServer:Option[ActorRef]=None
  def checkUsage:Boolean = {
    declaredUse==actualUse
  }
}
