package proptics

import cats.arrow.{Profunctor, Strong}
import cats.instances.function._
import cats.syntax.eq._
import cats.syntax.option._
import cats.{Applicative, Comonad, Eq}
import proptics.internal.{Forget, Re, Zipping}
import proptics.profunctor.Costar
import proptics.rank2types.Rank2TypeIsoLike
import proptics.syntax.FunctionSyntax._

import scala.Function.const
import scala.{Function => F}

/**
 * A generalized isomorphism
 *
 * @tparam S the source of an [[Iso_]]
 * @tparam T the modified source of an [[Iso_]]
 * @tparam A the target of an [[Iso_]]
 * @tparam B the modified target of a [[Iso_]]
 */
abstract class Iso_[S, T, A, B] extends Serializable { self =>
  private[proptics] def apply[P[_, _]](pab: P[A, B])(implicit ev: Profunctor[P]): P[S, T]

  def view[R](s: S): A = self[Forget[A, *, *]](Forget(identity[A])).runForget(s)

  def set(b: B): S => T = over(const(b))

  def over(f: A => B): S => T = self(f)

  def overF[F[_]: Applicative](f: A => F[B])(s: S): F[T] = traverse(s)(f)

  def traverse[F[_]](s: S)(f: A => F[B])(implicit ev: Applicative[F]): F[T] = ev.map(f(self.view(s)))(self.set(_)(s))

  def filter(f: A => Boolean): S => Option[A] = s => view(s).some.filter(f)

  def exists(f: A => Boolean): S => Boolean = f compose view

  def noExists(f: A => Boolean): S => Boolean = s => !exists(f)(s)

  def contains(s: S)(a: A)(implicit ev: Eq[A]): Boolean = exists(_ === a)(s)

  def notContains(s: S)(a: A)(implicit ev: Eq[A]): Boolean = !contains(s)(a)

  def zipWith[F[_]](f: A => A => B): S => S => T = self(Zipping(f))(Zipping.closedZipping).runZipping

  def zipWithF[F[_]: Comonad](fs: F[S])(f: F[A] => B)(implicit ev: Applicative[F]): T = {
    self(Costar(f))(Costar.profunctorCostar[F](ev)).runCostar(fs)
  }

  def re: Iso_[B, A, T, S] = new Iso_[B, A, T, S] {
    override def apply[P[_, _]](pab: P[T, S])(implicit ev: Profunctor[P]): P[B, A] =
      self(Re(identity[P[B, A]])).runRe(pab)
  }

  def asLens_ : Lens_[S, T, A, B] = new Lens_[S, T, A, B] {
    override private[proptics] def apply[P[_, _]](pab: P[A, B])(implicit ev: Strong[P]): P[S, T] = self(pab)
  }
}

object Iso_ {
  private[proptics] def apply[S, T, A, B](f: Rank2TypeIsoLike[S, T, A, B]): Iso_[S, T, A, B] = new Iso_[S, T, A, B] {
    override def apply[P[_, _]](pab: P[A, B])(implicit ev: Profunctor[P]): P[S, T] = f(pab)
  }

  def apply[S, T, A, B](get: S => A)(inverseGet: B => T): Iso_[S, T, A, B] = iso(get)(inverseGet)

  def iso[S, T, A, B](get: S => A)(inverseGet: B => T): Iso_[S, T, A, B] = {
    Iso_(new Rank2TypeIsoLike[S, T, A, B] {
      override def apply[P[_, _]](pab: P[A, B])(implicit ev: Profunctor[P]): P[S, T] = ev.dimap(pab)(get)(inverseGet)
    })
  }

  def curried[A, B, C, D, E, F]: Iso_[(A, B) => C, (D, E) => F, A => B => C, D => E => F] =
    iso[(A, B) => C, (D, E) => F, A => B => C, D => E => F](_.curried)(F.uncurried[D, E, F])

  def uncurried[A, B, C, D, E, F]: Iso_[A => B => C, D => E => F, (A, B) => C, (D, E) => F] =
    iso[A => B => C, D => E => F, (A, B) => C, (D, E) => F](F.uncurried[A, B, C])(_.curried)

  def flipped[A, B, C, D, E, F]: Iso_[A => B => C, D => E => F, B => A => C, E => D => F] =
    iso[A => B => C, D => E => F, B => A => C, E => D => F](_.flip)(_.flip)
}

object Iso {
  private[proptics] def apply[S, A](f: Rank2TypeIsoLike[S, S, A, A]): Iso[S, A] = new Iso[S, A] {
    override def apply[P[_, _]](pab: P[A, A])(implicit ev: Profunctor[P]): P[S, S] = f(pab)
  }

  def apply[S, A](get: S => A)(inverseGet: A => S): Iso[S, A] = Iso_.iso(get)(inverseGet)

  def iso[S, A](get: S => A)(inverseGet: A => S): Iso[S, A] = Iso_.iso(get)(inverseGet)

  /** If `A1` is obtained from `A` by removing a single value, then `Option[A1]` is isomorphic to `A` */
  def non[A](a: A)(implicit ev: Eq[A]): Iso[Option[A], A] = {
    def g(a1: A): Option[A] = if (a1 === a) None else a.some

    Iso_.iso((op: Option[A]) => op.getOrElse(a))(g)
  }
}