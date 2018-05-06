package com.calvin.kafkaexample.twitter

import com.calvin.kafkaexample.Kafka
import fs2.Pipe
import org.http4s.client.Client

object TwitterStream {
  import org.http4s._
  import org.http4s.client.blaze._
  import org.http4s.client.oauth1
  import cats.effect._
  import fs2.Stream
  import fs2.io.stdout
  import fs2.text.{lines, utf8Encode}
  import jawnfs2._
  import io.circe.{Printer}
  import cats.implicits._
  import cats.data.NonEmptyList
  import cats.effect.Effect
  import fs2.{Pipe, Segment, Stream}
  import io.circe.{Json, Printer}
  import org.http4s.{Headers, MediaType, Request, Uri}
  import com.vdurmont.emoji.{Emoji, EmojiManager, EmojiParser => JEmojiParser}
  import scala.collection.JavaConverters._
  import org.log4s.getLogger

  private[this] val logger = getLogger
  var tweetCount = 0
  // jawnstreamz needs to know what JSON AST you want
  implicit val f = io.circe.jawn.CirceSupportParser.facade


  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  def sign[F[_]](consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)(req: Request[F])(implicit F: Sync[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token    = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /* Sign the incoming `Request[IO]`, stream the `Response[IO]`, and `parseJsonStream` the `Response[IO]`.
   * `sign` returns a `IO`, so we need to `Stream.eval` it to use a for-comprehension.
   */

  def filterLeft[F[_], A, B](stream: Stream[F, Either[A, B]]): Stream[F, B] = stream.flatMap {
    case Right(r) => Stream.emit(r)
    case Left(_) => Stream.empty
  }

  def tweetPipeS[F[_]](jsonStream: Stream[F, Json]): Stream[F, BasicTweet] = filterLeft(jsonStream.map{ json =>
    json.as[BasicTweet].leftMap(pE => s"ParseError: ${pE.message} - ${json.pretty(Printer.noSpaces)}")
  })


  def stream[F[_]](consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String, client: Client[F])(req: Request[F])(implicit F: Sync[F]): Stream[F, BasicTweet] = for {
    sr  <- Stream.eval(sign[F](consumerKey, consumerSecret, accessToken, accessSecret)(req))
    res <- client.streaming(sr)(resp => tweetPipeS(resp.body.chunks.parseJsonStream))
  } yield res

  def processTweets[F[_]](stream: Stream[F, BasicTweet], kafka: Kafka) = {
    stream.repeat.map{tweet =>
      println(tweet.toString)
      kafka.queueTweets(tweet)
    }
  }

  def twitterStream[F[_]](client: Client[F], kafka: Kafka)(implicit F: Sync[F]) = {
    val req = Request[F](Method.GET, Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))
    val s = stream("hpeyM2PqcBvrFqt3nLNEaeWEF", "v5yB5l1S0KC7wmojJTNCdFo0uNVNPyQAHpmwJR8GKynxxVveLQ", "794382924690886656-USgje15x7EZGnzRLulNtLwyXznjYbt2", "93zUyplYSVaMOKoOjDFz1W4RF3LQdzUFlN7wgTL0OCnBZ", client)(req)
    processTweets(s, kafka).compile.drain
  }
}
