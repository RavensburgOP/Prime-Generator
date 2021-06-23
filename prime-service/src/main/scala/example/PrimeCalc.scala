package example.primestream.calc

import scala.concurrent.Future
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.google.protobuf.timestamp.Timestamp
import example.primestream.grpc._

class PrimesImpl(implicit mat: Materializer) extends Primes {
  import mat.executionContext

  def sieve(stream: Stream[Int]): Stream[Int] =
    stream.head #:: sieve(
      stream.tail
    ).filter(_ % stream.head != 0)

  def prime_stream(
      end: Long
  ): Source[example.primestream.grpc.Response, NotUsed] = {
    val start_stream = Stream.from(2)
    val prime_stream = sieve(start_stream)

    Source(
      prime_stream.takeWhile(_ <= end)
    )
      .map(next_prime => Response(next_prime))

  }

  override def primeStream(in: Request): Source[Response, NotUsed] = {
    // TODO: introduce some kind of result caching
    prime_stream(in.num)
  }
}
