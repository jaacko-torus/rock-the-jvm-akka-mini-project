package com.jaackotorus.bank

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.*

import actors.Bank
import actors.PersistentBankAccount.Response
import actors.PersistentBankAccount.Command.*
import actors.PersistentBankAccount.Response.*

@main
def bank: Unit = {
  val rootBehavior: Behavior[NotUsed] = Behaviors.setup(context => {
    val bank   = context.spawn(Bank(), "bank")
    val logger = context.log

    val responseHandler = context.spawn(
      Behaviors.receiveMessage[Response] {
        case BankAccountCreatedResponse(id) =>
          logger.info(s"successfully created bank account $id")
          Behaviors.same
        case GetBankAccountResponse(maybeBankAccount) =>
          logger.info(s"Account details: $maybeBankAccount")
          Behaviors.same
      },
      "replyHandler"
    )
    // ask pattern
    import akka.actor.typed.scaladsl.AskPattern.*
    implicit val timeout: Timeout                           = Timeout(2.seconds)
    implicit val scheduler: Scheduler                       = context.system.scheduler
    implicit val executionContext: ExecutionContextExecutor = context.executionContext

    Behaviors.empty
  })

  val system = ActorSystem(rootBehavior, "BankDemo")
}
