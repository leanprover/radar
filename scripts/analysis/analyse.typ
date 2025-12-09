#import "@preview/lilaq:0.5.0" as lq
#set page(margin: 1cm)

#let data = json("lean4.json")

#let pairs(values) = {
  let x = ()
  let y = ()
  for (i, (l, r)) in values.zip(values.slice(1)).enumerate() {
    if l == none or r == none { continue }
    x.push(l)
    y.push(r)
  }
  (x, y)
}

#let deltas(values) = {
  let deltas = ()
  for (l, r) in values.zip(values.slice(1)) {
    if l == none or r == none { continue }
    deltas.push(r - l)
  }
  deltas
}

#let indices(values) = range(0, values.len())

#let metrics = data.keys()
#let metrics = metrics.filter(it => it.starts-with("build/module/"))
#let metrics = metrics.filter(it => it.ends-with("//instructions"))

#let start = 0
#let batch = 4
#for metric in metrics.slice(start * batch, (start + 1) * batch) {
  let values = data.at(metric)
  align(center)[
    #raw(metric)

    #lq.diagram(
      title: "values",
      lq.scatter(..pairs(values)),
      // lq.scatter(..pairs(deltas(values))),
      // lq.plot(indices(deltas(values)), deltas(values)),
    )
    #lq.diagram(
      title: "deltas",
      lq.scatter(..pairs(deltas(values))),
    )
  ]
}
