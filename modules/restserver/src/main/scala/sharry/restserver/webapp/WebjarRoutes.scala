package sharry.restserver.webapp

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.NoopCacheStrategy
import org.http4s.server.staticcontent.WebjarService.{WebjarAsset, Config => WebjarConfig}

object WebjarRoutes {

  def appRoutes[F[_]: Effect](blocker: Blocker)(implicit
      C: ContextShift[F]
  ): HttpRoutes[F] =
    webjarService(
      WebjarConfig(
        filter = assetFilter,
        blocker = blocker,
        cacheStrategy = NoopCacheStrategy[F]
      )
    )

  def assetFilter(asset: WebjarAsset): Boolean =
    List(
      ".js",
      ".css",
      ".html",
      ".jpg",
      ".png",
      ".eot",
      ".json",
      ".woff",
      ".woff2",
      ".svg",
      ".map",
      ".otf",
      ".ttf",
      ".yml"
    ).exists(e => asset.asset.endsWith(e))

}
