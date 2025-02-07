package sharry.backend

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource, Timer}
import sharry.backend.auth.Login
import sharry.store.Store

import scala.concurrent.ExecutionContext
import sharry.backend.signup.OSignup
import sharry.backend.account._
import sharry.backend.alias.OAlias
import sharry.backend.share.OShare
import sharry.backend.job.PeriodicCleanup
import sharry.backend.mail.OMail
import emil.javamail.JavaMailEmil

trait BackendApp[F[_]] {

  def login: Login[F]

  def signup: OSignup[F]

  def account: OAccount[F]

  def alias: OAlias[F]

  def share: OShare[F]

  def mail: OMail[F]
}

object BackendApp {

  def create[F[_]: ConcurrentEffect: ContextShift](
      cfg: Config,
      blocker: Blocker,
      store: Store[F]
  ): Resource[F, BackendApp[F]] =
    for {
      accountImpl <- OAccount[F](store)
      loginImpl   <- Login[F](accountImpl)
      signupImpl  <- OSignup[F](store)
      aliasImpl   <- OAlias[F](store)
      shareImpl   <- OShare[F](store, cfg.share)
      mailImpl    <- OMail[F](store, cfg.mail, JavaMailEmil[F](blocker))
    } yield new BackendApp[F] {
      val login: Login[F]      = loginImpl
      val signup: OSignup[F]   = signupImpl
      val account: OAccount[F] = accountImpl
      val alias: OAlias[F]     = aliasImpl
      val share: OShare[F]     = shareImpl
      val mail: OMail[F]       = mailImpl
    }

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
      cfg: Config,
      connectEC: ExecutionContext,
      blocker: Blocker
  ): Resource[F, BackendApp[F]] =
    for {
      store   <- Store.create(cfg.jdbc, connectEC, blocker, true)
      backend <- create(cfg, blocker, store)
      _ <-
        PeriodicCleanup.resource(cfg.cleanup, cfg.signup, backend.share, backend.signup)
    } yield backend
}
