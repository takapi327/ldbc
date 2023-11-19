# Contributing

These guidelines are meant to be a living document that should be changed and adapted as needed. We encourage changes that makes it easier to achieve our goals in an efficient way.

## Tooling

LDBC is built with [sbt](https://github.com/sbt/sbt), and you should be able to jump right in by running `sbt test`.

Please make sure to run `sbt scalafmtAll` (and commit the results!) before opening a pull request. This will take care of running both scalafmt, ensuring that the build doesn't just immediately fail to compile your work.

All PRs and branches are built using GitHub Actions, covering a large set of permutations of JVM versions, Scala versions, and operating systems.

## General Workflow

This is the process for committing code into `master`. There are of course exceptions to these rules, for example minor changes to comments and documentation, fixing a broken build etc.

1. Before starting to work on a feature or a fix, you have to make sure that:
   i. There is a ticket for your work in the project's issue tracker. If not, create it first.
2. You should always perform your work in a Git feature branch. The branch should be given a descriptive name that explains its intent. Some teams also like adding the ticket number and/or the [GitHub](http://github.com) user ID to the branch name, these details is up to each of the individual teams.
3. When the feature or fix is completed you should open a [Pull Request](https://help.github.com/articles/using-pull-requests) on GitHub.
4. Pull Requests should be reviewed by other maintainers (as many as possible). Note that the maintainers can consist of external contributors. External contributors (e.g. EPFL or independent committers) are encouraged to participate in the review process.
5. After the review you should fix the issues as needed (pushing a new commit for new review etc.), iterating until the reviewers give their thumbs up.
6. Once the code has passed review the Pull Request can be merged into the `master` branch.

## Pull Request Requirements

For a Pull Request to be considered at all it has to meet these requirements:

1. Regardless if the code introduces new features or fixes bugs or regressions, it must have comprehensive tests.
2. scalafmt must be applied to all Scala source code
3. Source and binary compatibility must always be kept

If these requirements are not met then the code should not be merged into `master`, or even reviewed - regardless of how good or important it is. No exceptions.

## Work In Progress

It is ok to work on a public feature branch in the GitHub repository. Something that can sometimes be useful for early feedback etc. If so then it is preferable to name the branch accordingly. This can be done by either prefix the name with ``wip-`` as in ‘Work In Progress’, or use hierarchical names like ``wip/..``, ``feature/..`` or ``topic/..``. Either way is fine as long as it is clear that it is work in progress and not ready for merge. This work can temporarily have a lower standard. However, to be merged into `master` it will have to go through the regular process outlined above, with Pull Request, review etc..

Also, to facilitate both well-formed commits and working together, the ``wip`` and ``feature``/``topic`` identifiers also have special meaning.   Any branch labelled with ``wip`` is considered “git-unstable” and may be rebased and have its history rewritten.   Any branch with ``feature``/``topic`` in the name is considered “stable” enough for others to depend on when a group is working on a feature.
