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

  def sieve(end: Long): Source[example.primestream.grpc.Response, NotUsed] = {
    require(end >= 2)
    val odds = Stream.from(3, 2).takeWhile(_ <= Math.sqrt(end).toLong)
    val composites =
      odds.flatMap(i => Stream.from(i * i, 2 * i).takeWhile(_ <= end))

    Source(
      // TODO: find a more elegant way to add 2 at the beginning of the sequence
      //   TODO: This implementation will not return until all primes up to end has been found. Find a way to stream the results back as new prime numbers are found
      Stream
        .from(2)
        .takeWhile(_ <= 2)
        .concat(
          Stream
            .from(3, 2)
            .takeWhile(_ <= end)
            .diff(composites)
        )
    )
      .map(next_prime => Response(next_prime))

  }

  override def primeStream(in: Request): Source[Response, NotUsed] = {
    // TODO: introduce some kind of result caching
    sieve(in.num)
  }
}
