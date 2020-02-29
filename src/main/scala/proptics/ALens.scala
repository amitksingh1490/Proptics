package proptics

import cats.arrow.Strong
import cats.instances.function._
import cats.syntax.apply._
import proptics.internal.Shop

import scala.Function.const

/**
 * A [[Lens]] with fixed type [[Shop]] [[cats.arrow.Profunctor]]
 *
 * @tparam S the source of a [[ALens]]
 * @tparam T the modified source of a [[ALens]]
 * @tparam A the target of a [[ALens]]
 * @tparam B the modified target of a [[ALens]]
 */
abstract class ALens[S, T, A, B] { self =>
  def apply(shop: Shop[A, B, A, B]): Shop[A, B, S, T]

  def withLens[R](f: (S => A) => (S => B => T) => R): R

  def cloneLens[P[_, _]](implicit ev: Strong[P]): Lens[S, T, A, B] = {
    withLens(Lens[S, T, A, B])
  }

  /**
   * Converts a [[Lens]] into the form that [[Lens_]] accepts.
   *
   * Can be useful when defining a lens where the focus appears under multiple
   * constructors of an algebraic data type. This function would be called for
   * each case of the data type.
   */
  def lensStore(s: S): (A, B => T) = {
    withLens(sa => sbt => (sa, sbt).mapN(Tuple2.apply))(s)
  }
}

object ALens {
  private[proptics] def apply[S, T, A, B](f: Shop[A, B, A, B] => Shop[A, B, S, T]): ALens[S, T, A, B] = new ALens[S, T, A, B] { self =>
      override def withLens[R](f: (S => A) => (S => B => T) => R): R = {
        val shop = self(Shop(identity, const(identity)))

        f(shop.get)(shop.set)
      }

      override def apply(shop: Shop[A, B, A, B]): Shop[A, B, S, T] = f(shop)
  }

  def apply[S, T, A, B](get: S => A)(set: S => B => T): ALens[S, T, A, B] =
    ALens(shop => {
      Shop(shop.get compose get, s => b => {
        val a = get(s)
        val b2 = shop.set(a)(b)

        set(s)(b2)
      })
    })
}

object ALens_ {
  def apply[S, A](get: S => A)(set: S => A => S): ALens_[S, A] = ALens(get)(set)
}



