# Messages used in the Dough Preparation Stage

## Kneading Request

#### Content
```
{
    "kneadingTime": float,
    "productType": String,
    "guids": Vector<String>
}

```

#### Example Content


```
{
    "kneadingTime":1.0,
    "productType":"Berliner",
    "guids":["order-001","order-002"]
}

```

## Kneading Notification

#### Content
```
{
    "productType": String,
    "guids": Vector<String>
}

```

#### Example Content


```
{
    "productType":"Berliner",
    "guids":["order-001","order-002"]
}

```

## Preparation Request

#### Content
```
{
    "productQuantities": Vector <int>,
    "steps": Array JSON Objects,
    "productType": String,
    "guids": Vector<String>
}

```
Step
```
{
    "action": String,
    "duration": float
}
```


#### Example Content


```
{
    "productQuantities":[10,20],
    "steps":[
        {
        "action":"doSomething",
        "duration":2.0
        },

        {   
        "action":"doSomethingElse",
        "duration":3.0
        }
    ],
    "productType":"a product type",
    "guids":["GUID1","GUID2"]
}


```

## Preparation Notification

#### Content
```
{
    "productType": String,
    "guids": Vector<String>
}

```

#### Example Content


```
{
    "productType":"Berliner",
    "guids":["order-001","order-002"]
}

```

## Proofing Request

#### Content

```
{
    "proofingTime": float,
    "productType": String,
    "guids": Vector<String>
}

```

#### Example Content


```
{
    "proofingTime":1.0,
    "productType":"Berliner",
    "guids":["order-001","order-002"]
}

```

## Dough Notification Message

#### Content

```
{
    "productType": String,
    "quantity": int,
    "guids": Vector<String>
}
```

#### Example Content

```
{
    "productType":"Berliner",
    "quantity: 5,
    "guids":["order-001","order-002"]
}
```
