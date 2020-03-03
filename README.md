JRedis Notifier Plugin
======================

Descripcion
-----
Para los pipeline de tipo declarativo expone una variable global llamada `jredis` que permite enviar informacion y/o objetos en  formato `JSON` directamente a un servidor redis.


Requerimientos
----------------------

**Prerequisitos**: Es necesario configurar la conexion hacia el servidor redis desde las configuraciones globales de Jenkins indicando `URL` y `PORT` como un string en formato: `http://localhost:6379`

--------------------
Examples:

```Groovy
stage('Test: Redis') {
    steps {
        script {
            jredis.set('k1', ['Hello':'World'])
            echo jredis.get('k1')
        }
    }
}
```


```Groovy
stage('Test: Redis') {
    steps {
        script {
            def myObject = [:]
            myObject.put("k_1", "level 1")
            myObject.put("k_2", ["item1","item2", ["k_3":"level 2"]])

            jredis.set('messages', myObject)
            echo jredis.get('messages')
        }
    }
}
```
