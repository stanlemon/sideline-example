# Sideline Example

This repository includes an example Storm topology that uses the sideline implementation included with the Dynamic Spout Framework as well as some command line tools that can be used to demo sidelining.

---------------------------------------

Run the example topology that uses sidelining.

```
mvn clean compile exec:java@topology
```

Generate some keyed messages, they are randomly distributed.
```
mvn clean compile exec:java@producer -Dexec.args="-n 50 -s 1000"
```

Start a sideline for key2.
```
mvn clean compile exec:java@sideline -Dexec.args="-t start -c Stan Lemon -r Testing -d {\"filteredKeys\":[\"key2\"]}"
```

Resume a sideline for key2.
```
mvn clean compile exec:java@sideline -Dexec.args="-t resume -i 6469C0AEF31751437498A4B9A99D3A4E"
```

Resolve a sideline for key2.
```
mvn clean compile exec:java@sideline -Dexec.args="-t resolve -i 6469C0AEF31751437498A4B9A99D3A4E"
```
