# LockExp
Benchmark Lock and Universal Construction implementation.
```
Usage: Locks Experiments options_list
Options: 
    --min, -m [1] -> Min number of threads { Int }
    --max, -M [8] -> Max number of threads { Int }
    --step, -s [1] -> Step for threads { Int }
    --limit, -l [1000000] -> Limit for counter { Int }
    --repeats, -r [10] -> Number of trials { Int }
    --locks, -L -> Locks to test { Value should be one of [ALock, BackoffLock, CLHLock, CompositeLock, CompositeFastPathLock, HBOLock, HCLHLock, MCSLock, TASLock, TTASLock, TOLock] }
    --universals, -U -> Universals to test { Value should be one of [LFUniversal, WFUniversal, LFUniversalBack, WFUniversalBack] }
    --output, -o [plot] -> Output file { String }
    --format, -f [html] -> Output format { Value should be one of [png, html, svg, jpeg, tiff] }
    --minBackoff, -b [1] -> Min backoff { Int }
    --backoffTrials, -T [10] -> backoff trials { Int }
    --help, -h -> Usage info
```