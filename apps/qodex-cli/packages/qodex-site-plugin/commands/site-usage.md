---
description: Show Qodex Sites usage and plan limits.
skills: site-deploy
---

Use the `site-deploy` skill for this request:

$ARGUMENTS

Run `qodex-site usage` and show traffic, requests and storage against the plan limits for the current period. If `canDeploy` is false, explain `exceededReasons`: new deployments are paused until the next period, and upgrading the Qodex plan raises the limits.
