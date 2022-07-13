package com.jaackotorus.bank
package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import java.util.UUID

import PersistentBankAccount.{Command, Response}
import PersistentBankAccount.Command.*
import PersistentBankAccount.Response.*

object Bank {
  enum Event:
    case BankAccountCreated(id: String)

  case class State(accounts: Map[String, ActorRef[Command]])

  import Event.*

  def commandHandler(
      context: ActorContext[Command]
  )(state: State, command: Command): Effect[Event, State] = command match {
    case command @ CreateBankAccount(_, _, _, _) => {
      val id             = UUID.randomUUID().toString
      val newBankAccount = context.spawn(PersistentBankAccount(id), id)

      Effect.persist(BankAccountCreated(id)).thenReply(newBankAccount)(_ => command)
    }
    case command @ UpdateBalance(id, currency, amount, replyTo) => {
      state.accounts.get(id) match {
        case Some(account) => Effect.reply(account)(command)
        case None          => Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(None))
      }
    }
    case command @ GetBankAccount(id, replyTo) => {
      state.accounts.get(id) match {
        case Some(account) => Effect.reply(account)(command)
        case None          => Effect.reply(replyTo)(GetBankAccountResponse(None))
      }
    }
  }

  def eventHandler(context: ActorContext[Command])(state: State, event: Event): State =
    event match {
      case BankAccountCreated(id) => {
        val account = context
          // exists after command handler
          // `.child` erases type `Command`
          .child(id)
          // does not exist in the recovery mode, so needs to be created
          .getOrElse(context.spawn(PersistentBankAccount(id), id))
          .asInstanceOf[ActorRef[Command]]

        state.copy(state.accounts + (id -> account))
      }
    }

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}
