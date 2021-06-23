import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.http.scaladsl.Http
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import akka.http.scaladsl.server.Directives._
import scala.concurrent.Future
import scala.io.StdIn
import example.primestream.grpc._

object PrimeProxy {

  def getPrimeNumbers(limit: Int): Source[Int, akka.NotUsed] =
    Source.fromIterator(() => Iterator(2, 3, 5, 7, 11, 13, 17))

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "PrimeNumbers")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val clientSettings =
      GrpcClientSettings.connectToServiceAt("127.0.0.1", 8081).withTls(false)
    val client = PrimesClient.apply(clientSettings)

    val route =
      pathPrefix("prime") {
        path(LongNumber) { int =>
          get {
            complete(
              HttpEntity(
                ContentTypes.`text/plain(UTF-8)`,
                // transform each number to a chunk of bytes
                client
                  .primeStream(Request(int))
                  .map(response => response.result)
                  .map(n => ByteString(s"$n\n"))
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
      .onComplete(_ => {
        client.close(); system.terminate()
      }) // and shutdown when done
  }

}
