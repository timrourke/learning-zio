package hello

import java.io.IOException

import zio.console.{Console, getStrLn, putStrLn}
import zio.{ExitCode, URIO, ZIO}

object Hello extends zio.App {

  val myAppLogic: ZIO[Console, IOException, Unit] =
    for {
      _ <- putStrLn("Hello! What is your name?")
      name <- getStrLn
      _ <- putStrLn(s"Hello, ${name}, welcome to ZIO!")
      num <- ZIO.succeed(42)
      _ <- putStrLn(s"The number is: $num")
      now <- ZIO.effectTotal(System.currentTimeMillis())
      _ <- putStrLn(s"Current time: $now")
    } yield ()

  def run(args: List[String]): URIO[Console, ExitCode] =
    myAppLogic.exitCode
}
