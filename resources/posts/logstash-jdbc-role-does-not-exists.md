This is more of a note to the future myself rather than a proper post.
However...

I've got an error trying to synchronize elasticsearch and postgresql
using logstash.  My configurations was along the lines of

```logstash
input {
    jdbc {
        jdbc_connection_string => "jdbc:postgresql://192.168.2.156:5432/chiaki"
        jdbc_user => "chiaki"
        jdbc_driver_library => "/path/to/postgresql-42.2.12.jre7.jar"
        jdbc_driver_class => "org.postgresql.Driver"
        jdbc_validate_connection => true
        statement => "select * from foo"
    }
}

output {
    elasticsearch {
        hosts => ["http://localhost:9200"]
        index => "foo"
        document_id => "%{the_pk}"
        doc_as_upsert => true
    }
}
```

But it error'd with something like "role _logstash does not exists."

I haven't really tracked down the issued, but found a workaround:
specify the user *also* in the connection string.

It's quite strange, since the `jdbc_user` params is required.

P.S. the error was about a missing `_logstash` role because on OpenBSD
that user is created for logstash.  This was a hint that `jdbc_user`
wasn't respected.
