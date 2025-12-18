<script setup lang="ts">
import CSection from "@/components/CSection.vue";
</script>

<template>
  <CSection title="About Radar" class="max-w-[80ch]">
    <p>
      Radar (<a
        href="https://github.com/leanprover/radar"
        class="decoration-blue underline decoration-dotted hover:decoration-solid"
        >source</a
      >) is a continuous benchmarking tool meant to help catch performance regressions and improvements. It
      automatically benchmarks every commit on the main branch of a repository and compares it to previous commits to
      detect changes in performance. It also provides comparisons between arbitrary commits, a graph view of metrics
      over time, and GitHub and Zulip integration.
    </p>
    <p>
      When Radar benchmarks a commit, it runs a repo-specific benchmark suite on one or more benchmark runner machines.
      The benchmark suite produces measurements for a set of metrics, which Radar then collects and stores. New commits
      on a repo's main branch are automatically added to the benchmark queue. Commits can also be enqueued using the
      GitHub bot.
    </p>
  </CSection>

  <CSection title="Significance Detection" class="max-w-[80ch]">
    <p>
      For all new commits on the repo's main branch, Radar tries to detect significant changes in performance by
      comparing the commit against its predecessor in the (linearized) commit history.
    </p>
    <p>
      Each metric's value is compared against the previous commit's value using a combination of hand-tuned thresholds
      and historical data. Based on those, the metrics that changed significantly are grouped into three categories:
      Small, medium, and large changes. If a commit accumulates a small number of large changes, a medium number of
      medium changes, or a large number of small changes, it itself becomes significant.
    </p>
    <p>
      Significant commits are highlighted on a repo's overview page. If Zulip integration is enabled for a repo,
      significant commits will trigger a message in the configured Zulip stream explaining the changes.
    </p>
  </CSection>

  <CSection title="GitHub Bot" class="max-w-[80ch]">
    <p>
      If GitHub integration is enabled for a repo, users can issue the command
      <i class="whitespace-nowrap">!bench</i> or <i class="whitespace-nowrap">!radar</i> in a pull request to benchmark
      their changes. Radar will enqueue the required commits and reply with the benchmark results as soon as they're
      available. The last few GitHub commands can be found on a repo's overview page.
    </p>
    <p>
      The GitHub bot commands must be issued in a comment on the pull request. The comment may contain additional text,
      as long as the command is in a line of its own. Multiple commands must be issued in separate comments.
    </p>
    <p>
      In the lean4 repository, a variation of this command called <i class="whitespace-nowrap">!bench mathlib</i> is
      available. Instead of running the lean4 benchmark suite, it benchmarks the nightly mathlib against the pull
      request's changes.
    </p>
    <p>
      In the mathlib4 repository, if the tag <i class="whitespace-nowrap">awaiting-CI</i> is present, radar waits until
      its is gone before starting to benchmark the pull request. You can use the tag if there is a high likelihood that
      CI won't pass but you still want to get benchmark results automatically. This replaces the old
      <i class="whitespace-nowrap">bench-after-CI</i> tag workflow.
    </p>
  </CSection>
</template>
