import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import scala.io.StdIn

object PrimeProxy {

  def getPrimeNumbers(limit: Int): Source[Int, akka.NotUsed] =
    Source.fromIterator(() => Iterator(2, 3, 5, 7, 11, 13, 17))

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "PrimeNumbers")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val route =
      pathPrefix("prime") {
        path(IntNumber) { int =>
          get {
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                // transform each number to a chunk of bytes
                getPrimeNumbers(int).map(n => ByteString(s"$n\n"))
              )
            )
          }
        }
      }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
