package sbt

import sbt.internal.AddSettings
import scala.collection.breakOut

/**
 * Defined in sbt packaged because of private[sbt] restrictions.
 */
class ProjectDefinitionUtil(project: ProjectDefinition[_]) {

  def sbtFiles = {
    def sbtFiles(addSettings: AddSettings): Set[File] = addSettings match {
      case addSettings: AddSettings.SbtFiles =>
        addSettings.files.map(IO.resolve(project.base, _)).filterNot(_.isHidden).toSet
      case addSettings: AddSettings.DefaultSbtFiles =>
        BuildPaths.configurationSources(project.base).filter(addSettings.include).filterNot(_.isHidden).toSet
      case addSettings: AddSettings.Sequence =>
        addSettings.sequence.flatMap(sbtFiles)(breakOut)
      case _ => Set.empty
    }
    sbtFiles(AddSettings.defaultSbtFiles)
  }

}
