import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by prophet on 07.02.15.
 */
class Client2 (entryPoint:ActorRef,trans:ActorRef) extends Actor{
  implicit val timeout = Timeout(10 seconds)
  val objectsToUse = mutable.Map[String,PackagedObject]()
  trans ! Claim()

  askForObject("OBIEKT A",2)
  askForObject("OBIEKT B",3)
  askForObject("OBIEKT C",4)


  def askForObject(name:String, supremum:Int) = {
    var ob = trans ? Access(name,supremum)
    var result = Await.result(ob,Duration.Inf)
    result match {
      case msg:AccessReply => {
//        println(self+ "Package received. "+msg.pack.proxy.innerObj.declaredUse)
        objectsToUse+=(name->msg.pack)
      }
      case _ => {
        println("Unexpected reply")
      }
    }
  }

  def executeAction(objName:String) = {
    println(self+ "WRITING "+objName+". CURRENT VALUE IS "+objectsToUse(objName).proxy.innerObj.value)
    objectsToUse(objName).write(23.0)
    println(self+ " "+objName+" VALUE AFTER WRITE: "+objectsToUse(objName).read)
    println(self+ "TRYING TO READ  "+objName+" AGAIN.")
    println(self+ " VALUE: "+objectsToUse(objName).read)
    objectsToUse(objName).commit(objName)
  }

  def receive = {
    case msg:Ready => {
      if(objectsToUse.contains(msg.obj.name)){
        objectsToUse(msg.obj.name).proxy.innerObj=msg.obj
        objectsToUse(msg.obj.name).ready=true
      }
      executeAction(msg.obj.name)
    }
    case msg:UpdateProxy => {
      objectsToUse(msg.proxy.innerObj.name).proxy=msg.proxy
      objectsToUse(msg.proxy.innerObj.name).updated=true
    }
  }
}
