import akka.actor._
import akka.io.UdpConnected.Connect
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask


object Coordinator extends App{
  val servers = mutable.Set[ActorRef]()
  implicit val timeout = Timeout(10 seconds)
  val system = ActorSystem("Supreme")
  val srvList = scala.collection.mutable.Map[Int,ActorRef]()

  val server = system.actorOf(Props(new Server(servers)))
  servers+=server
  Thread.sleep(500)

  val server2 = system.actorOf(Props(new Server(servers)))
  servers+=server2
  Thread.sleep(500)

  val server3 = system.actorOf(Props(new Server(servers)))
  servers+=server3
  Thread.sleep(500)

  val transaction = system.actorOf(Props(new Transaction(server)))
  val client = system.actorOf(Props(new Client(server,transaction)))

  val transaction2= system.actorOf(Props(new Transaction(server)))
  val client2 = system.actorOf(Props(new Client2(server2,transaction2)))

  val transaction3= system.actorOf(Props(new Transaction(server)))
  val client3 = system.actorOf(Props(new Client3(server3,transaction3)))

  //  system.shutdown
}