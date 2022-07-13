package com.jaackotorus.bank
package actors

import akka.actor.typed.ActorRef
import akka.persistence.typed.scaladsl.Effect
import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId

object PersistentBankAccount {
  // commands = messages
  enum Command:
    case CreateBankAccount(
        user: String,
        currentcy: String,
        initialBalance: Double, // only positive
        replyTo: ActorRef[Response]
    )
    case UpdateBalance(
        id: String,
        currency: String,
        amount: Double, // can be negative
        replyTo: ActorRef[Response]
    )
    case GetBankAccount(id: String, replyTo: ActorRef[Response])

  // events = to persist to Cassandra
  enum Event:
    case BankAccountCreated(bankAccount: BankAccount)
    case BalanceUpdated(amount: Double)

  // state
  case class BankAccount(id: String, user: String, currency: String, balance: Double)

  // responses
  enum Response:
    case BankAccountCreatedResponse(id: String)
    case BankAccountBalanceUpdatedResponse(bankAccount: Option[BankAccount])
    case GetBankAccountResponse(bankAccount: Option[BankAccount])
}

class PersistentBankAccount {
  import PersistentBankAccount.{BankAccount, Command, Event, Response}
  import Command.*
  import Event.*
  import Response.*
  // command handler = message handler => persist an event
  // event handler => update state
  // state
  val commandHandler: (BankAccount, Command) => Effect[Event, BankAccount] = (state, command) =>
    command match {
      case CreateBankAccount(user, currency, initialBalance, bank) => {
        val id = state.id

        Effect
          .persist(BankAccountCreated(BankAccount(id, user, currency, initialBalance)))
          .thenReply(bank)(_ => BankAccountCreatedResponse(id))
      }
      case UpdateBalance(_, _, amount, bank) => {
        val newBalance = state.balance + amount

        if (newBalance < 0)
          // illegal
          Effect.reply(bank)(BankAccountBalanceUpdatedResponse(None))
        else
          Effect
            .persist(BalanceUpdated(newBalance))
            .thenReply(bank)(newState => BankAccountBalanceUpdatedResponse(Some(newState)))
      }
      case GetBankAccount(_, bank) => {
        Effect.reply(bank)(GetBankAccountResponse(Some(state)))
      }
    }

  val eventHandler: (BankAccount, Event) => BankAccount = (state, event) =>
    event match {
      case BankAccountCreated(bankAccount) => bankAccount
      case BalanceUpdated(amount)          => state.copy(balance = state.balance + amount)
    }

  def apply(id: String): Behavior[Command] = EventSourcedBehavior[Command, Event, BankAccount](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = BankAccount(id, "", "", 0.0),
    commandHandler = commandHandler,
    eventHandler = eventHandler
  )
}
