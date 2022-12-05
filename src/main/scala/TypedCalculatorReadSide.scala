import CalculatorRepo.{getLatestOffsetAndResult, initDataBase, updateResultAndOfsset}
import TypedCalculatorWriteSide._
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.scaladsl.{Flow, Source}

case class TypedCalculatorReadSide(system: ActorSystem[NotUsed]) {
  initDataBase

  implicit val materializer            = system.classicSystem
  var (offset, latestCalculatedResult) = getLatestOffsetAndResult
  val startOffset: Int                 = if (offset == 1) 1 else offset + 1

  val readJournal: CassandraReadJournal =
    PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val source: Source[EventEnvelope, NotUsed] = readJournal
    .eventsByPersistenceId("001", startOffset, Long.MaxValue)

  private case class BusinessLogicFlowResult(latestCalculatedResult: Double, sequenceNr: Long)

  private val businessLogicFlow: Flow[EventEnvelope, BusinessLogicFlowResult, NotUsed] = Flow[EventEnvelope].map { e =>
    e.event match {
      case Added(_, amount) =>
        latestCalculatedResult += amount
        BusinessLogicFlowResult(latestCalculatedResult, e.sequenceNr)
      case Multiplied(_, amount) =>
        latestCalculatedResult *= amount
        BusinessLogicFlowResult(latestCalculatedResult, e.sequenceNr)
      case Divided(_, amount) =>
        latestCalculatedResult /= amount
        BusinessLogicFlowResult(latestCalculatedResult, e.sequenceNr)
    }
  }

  private val dbLogicFlow: Flow[BusinessLogicFlowResult, Unit, NotUsed] =
    Flow[BusinessLogicFlowResult].map(r => updateResultAndOfsset(r.latestCalculatedResult, r.sequenceNr))

  source.async.map { x =>
    println(x.toString())
    x
  }
    .via(businessLogicFlow).async
    .via(dbLogicFlow).async
    .run()
}
