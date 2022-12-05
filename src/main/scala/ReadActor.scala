import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object ReadActor {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup(_ => Behaviors.same)

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[NotUsed] = ActorSystem(ReadActor(), "akka_typed")

    TypedCalculatorReadSide(system)
  }
}
