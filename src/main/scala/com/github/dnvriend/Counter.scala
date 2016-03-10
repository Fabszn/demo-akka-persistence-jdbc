/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import akka.NotUsed
import akka.actor.{ ActorSystem, Actor, ActorRef, Props }
import akka.persistence.jdbc.query.journal.scaladsl.JdbcReadJournal
import akka.persistence.query.scaladsl.ReadJournal
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.github.dnvriend.CounterActor.Incremented

import scala.concurrent.{ ExecutionContext, Future }

object CounterActor {
  sealed trait Command
  final case class Increment(value: Int) extends Command
  final case class Decrement(value: Int) extends Command

  sealed trait Event {
    def value: Int
  }
  final case class Incremented(value: Int) extends Event
  final case class Decremented(value: Int) extends Event

  case class CounterState(value: Int = 0) {
    def update(event: Event): CounterState = event match {
      case _: Incremented ⇒ copy(value + event.value)
      case _: Decremented ⇒ copy(value - event.value)
    }
  }
}

class CounterActor extends PersistentActor {
  import CounterActor._
  override def persistenceId: String = "Counter"

  private var state = CounterState()

  private def handleEvent(event: Event): Unit = {
    state = state.update(event)
    println("Current state: " + state)
  }

  override def receiveRecover: Receive = {
    case event: Incremented                       ⇒ handleEvent(event)
    case event: Decremented                       ⇒ handleEvent(event)
    case SnapshotOffer(_, snapshot: CounterState) ⇒ state = snapshot
  }

  override def receiveCommand: Receive = {
    case Increment(value) ⇒
      println(s"==> Incrementing with: $value")
      persist(Incremented(value))(handleEvent)

    case Decrement(value) ⇒
      println(s"==> Decrementing with: $value")
      persist(Decremented(value))(handleEvent)

    case "snap" ⇒ saveSnapshot(state)
  }
}

object CounterView {
  case object Get
}

class CounterView(readJournal: JdbcReadJournal)(implicit ec: ExecutionContext, mat: Materializer) extends Actor {
  import akka.pattern.pipe

  def source: Source[EventEnvelope, NotUsed] = readJournal.currentEventsByPersistenceId("Counter", 0, Long.MaxValue)

  override def receive: Receive = {
    case CounterView.Get ⇒
      println("==> Getting the results")
      val sendTo = sender()
      getAllIncrements.pipeTo(sendTo)
  }

  def getAllIncrements: Future[List[Incremented]] = {
    source.runFold(List.empty[Incremented])((result: List[Incremented], envelope: EventEnvelope) ⇒ {
      envelope match {
        case EventEnvelope(offset, persistenceId, sequenceNr, value: Incremented) ⇒
          value :: result
      }
    })
  }
}

class Scheduler(counter: ActorRef, counterView: ActorRef)(implicit ec: ExecutionContext) extends Actor {
  import scala.concurrent.duration._
  def scheduleCount(): Unit = {
    context.system.scheduler.scheduleOnce(1.seconds, self, "count")
    println(s"==> Scheduling a count ($count)")
    count += 1
  }

  scheduleCount()

  var count: Long = 0

  override def receive: Actor.Receive = {
    case "count" ⇒
      if (count % 2 == 0) {
        counter ! CounterActor.Increment(1)
        scheduleCount()
      } else if (count % 3 == 0) {
        counter ! CounterActor.Decrement(1)
        scheduleCount()
      } else {
        //        counterView ! CounterView.Get
        scheduleCount()
      }
    case xs: List[_] ⇒
      println(s"==> Received events: $xs")
      scheduleCount()
  }
}

object Counter extends App with Core {
  override def resourceName: String = "counter-application.conf"

  val counter = system.actorOf(Props(new CounterActor))
  val counterView = system.actorOf(Props(new CounterView(readJournal)))
  val scheduler = system.actorOf(Props(new Scheduler(counter, counterView)))

  // async event listener
  readJournal.eventsByPersistenceId("Counter", 0, Long.MaxValue).runForeach {
    case EventEnvelope(offset, pid, seqNo, event) ⇒
      println(s"==> Event added: offset: $offset, pid: $pid, seqNo: $seqNo, event: $event")
  }

  val banner = s"""
                  |Counter
                  |=======
                  |#####  ###### #    #  ####
                  |#    # #      ##  ## #    #
                  |#    # #####  # ## # #    #
                  |#    # #      #    # #    #
                  |#    # #      #    # #    #
                  |#####  ###### #    #  ####
                  |
                  |$BuildInfo
                  |
  """.stripMargin

  println(banner)
}
