package example.primestream.calc

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.stream.scaladsl.{Sink, Source}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings

import com.typesafe.config.ConfigFactory

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent._

import example.primestream.grpc._

class PrimesSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig =
    PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  val service = new PrimesImpl()

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "PrimesImpl" should {
    "reply to single request" in {
      val reply =
        service
          .primeStream(Request(17))
          .runWith(Sink.seq)
          .futureValue
      val exp = Seq(2, 3, 5, 7, 11, 13, 17).map(Response(_))
      assert(reply == exp)
    }
  }
}
