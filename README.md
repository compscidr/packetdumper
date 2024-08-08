# packetdumper
A kotlin / android compatible buffer / packet dumper.

## Usage
Add the dependency to your project (coming soon on maven central):
```
implementation("com.jasonernst.packetdumper:packetdumper:<version>")
```

### pcapng tcp server
This will start a TCP server on port 19000 that will accept connections from wireshark as follows:
`wireshark -k -i TCP@<ip>:19000`

```kotlin
val dumper = PcapNgTcpServerPacketDumper()
dumper.start()
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)

//    ...

dumper.stop()
```

### pcapng file
Note that the file will actually be created with timestamps in the filename so that multiple runs
will not overwrite each other.
```kotlin
val dumper = PcapNgFilePacketDumper("/tmp", "test", "pcapng")
dumper.open()
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
dumper.close()
```

### hexdump to file
The following will dump in a format which is compatible with a wireshark hexdump import.
This assumes that the buffer contains an ipv4 packet. If your buffer has an ethernet frame already
just leave this as null. 
```kotlin
val dumper = TextFilePacketDumper("/tmp", "test", "txt")
dumper.open()
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
dumper.dumpBuffer(buffer, 0, buffer.limit(), true, EtherType.IPv4)
dumper.close()
```

### hexdump to stdout
```kotlin
val dumper = StringPacketDumper(writeToStdOut = true)
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
dumper.dumpBuffer(buffer, 0, buffer.limit(), true, EtherType.IPv4)
```

### hexdump to slf4j logger
This will log at the info level to the slf4j logger provided.
```kotlin
val logger = LoggerFactor.getLogger("somelogger")
val dumper = StringPacketDumper(logger)
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
dumper.dumpBuffer(buffer, 0, buffer.limit(), true, EtherType.IPv4)
```

### hexdump to string
```kotlin
val dumper = StringPacketDumper()
val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
val hexString = dumper.dumpBufferToString(buffer, 0, buffer.limit(), true, EtherType.IPv4)
println(hexString)
```

## Currently supports:
- [x] Basic buffer dumping capabilities to:
-   [x] hexdump stdout
-   [x] hexdump file (wireshark import compatible)
-   [x] hexdump string (for debugging)
-   [x] hexdump to slf4j logger
-   [x] pcapng file writing
-   [x] pcapng tcp server writing 
- [x] Tests + CI integration

## TODO
- [ ] Release on Maven Central
- [ ] Support options for pcap blocks
- [ ] Timestamps on enhanced packet blocks