# callgraph
Extracts a callgraph from one or more jar files, exploiting only statically available information.

Main class: `it.lucaneg.callgraph.Runner`.
Help message (execute with no args to print this):
```
usage: Runner [-c] [-f <format>] -i <path> -o <path> [-u]
 -c,--chop-types           use simple class names for return types and
                           parameters instead of fully qualified ones
 -f,--format <format>      the type of output graph. Defaults to graphml.
                           Possible values: graphml, dot
 -i,--input <path>         add a jar file to be analyzed
 -o,--output <path>        name of the output dot graph
 -u,--exclude-unresolved   do not dump unresolved (i.e. library) methods
 ```

### graphml attributes

When selecting graphml output format, signature and class name of each method will be stored in property `label` of the respective node. Properties `entry` and `exit` mean the method is an entrypoint (i.e. it has no callers, but it calls other methods) or an exitpoint (it has callers, but id does not call any other method). 
