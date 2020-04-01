package truss.interfaceAdaptor.aggregate.persistence

import com.github.j5ik2o.akka.persistence.s3.resolver.{ BucketNameResolver, PersistenceId }

class TrussBucketNameResolver extends BucketNameResolver {
  override def resolve(persistenceId: PersistenceId): String = "wallet"
}
