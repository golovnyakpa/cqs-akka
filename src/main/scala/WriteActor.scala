import TypedCalculatorWriteSide._
import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.persistence.typed.PersistenceId

object WriteActor {

  val persId: PersistenceId = PersistenceId.ofUniqueId("001")

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val writeActorRef: ActorRef[TypedCalculatorWriteSide.Command] =
        ctx.spawn(TypedCalculatorWriteSide(), "Calculato", Props.empty)

      writeActorRef ! Add(42)
      writeActorRef ! Multiply(2)

      Behaviors.same
    }

  def main(args: Array[String]): Unit =
    ActorSystem(WriteActor(), "writeActorProcess")
}
