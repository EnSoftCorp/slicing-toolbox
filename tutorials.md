---
layout: page
title: Tutorials
permalink: /tutorials/
---

If you haven't already, [install](/slicing-toolbox/install) the Slicing Toolbox plugin into Eclipse.

## Smart View Interactions

The Slicing Toolbox provides Atlas Smart Views for creating and viewing slice results. First, navigate to `Atlas` &gt; `Open Smart View`.  In the Smart View selection window select either the `Control Dependence Slice` or `Data Dependence Slice`, or `Program Dependence Slice`.

In the source editor, select a program statement and the Smart View will automatically update with the resulting CDG, DDG, or PDG slice. The nearest source statement will be used as the slicing criteria.

![Source Selections](../images/source-selections.png)

Optionally, multiple slicing criteria can be selected by pressing the `Shift` key and selecting each slicing criterion. For CDG slices one or more Control Flow nodes must be selected as the slicing criteria. For DDG and PDG slices one or more Data Flow nodes must be selected as the slicing criteria.

![Graph Selections](../images/graph-selections.png)

## Dependence Graph Factory

The Slicing Toolbox can be integrated into other client analyses. The easiest way to create a dependence graph is to use the `DependenceGraph.Factory` class. Let's use the factory to create dependence graphs and some of the intermediate computations for the simple program example shown below.

{% gist 0d1ca8392910ae7a5fb5f0de150d6591 %}

The factory creates an intra-procedural dependence graph for a given method.

	GraphElement method = ...

First we create the Control Dependence Graph (CDG).

	ControlDependenceGraph cdg = DependenceGraph.Factory.buildCDG(method);

The CDG is constructed using the Ferrante, Ottenstein, and Warren (FOW) algorithm, which augments the Control Flow Graph (CFG) with a master entry and exit node. An "augmentation" node is added with an edge to both the master entry and exit nodes.

	show(cdg.getAugmentedControlFlowGraph())


![Augmented CFG](../images/augmented-cfg.png)

 Using the augmented CFG a forward dominance analysis (post-dominance) is performed.
 
  	show(cdg.getForwardDominanceTree())


![Forward Dominance Tree](../images/fdt.png)

  	show(cdg.getGraph())

For each edge (X -&gt; Y) in augmented CFG, we find the nodes in the forward dominance tree from Y to the least common ancestor (LCA) of X and Y. We include LCA if LCA is X and exclude LCA if LCA is not X. For the resulting set of nodes, we add a control dependence edge from X to the node.

![Control Dependence Graph](../images/cdg.png)

	DataDependenceGraph ddg = DependenceGraph.Factory.buildDDG(method);

The DDG is computed by adding data dependence edges to statements that contain data flow nodes with a data flow dependence. Atlas provides the [defintion-use chain](https://en.wikipedia.org/wiki/Use-define_chain) by leveraging a [static single assignment form](https://en.wikipedia.org/wiki/Static_single_assignment_form). 

  	show(ddg.getGraph())


![Data Dependence Graph](../images/ddg.png)

The Program Dependence Graph (PDG) is created by combining the Control Dependence Graph and Data Dependence Graph (CDG union DDG).

	ProgramDependenceGraph pdg = DependenceGraph.Factory.buildPDG(method);
	show(pdg.getGraph())

![Program Dependence Graph](../images/pdg.png)

## Impact Analysis

While a reverse program slice shows what was relevant to compute the values of the selected data flow nodes (slicing criteria), a forward program slice shows what would be impacted if a data flow node value is changed.

![Impact Analysis](../images/impact-analysis.png)

## Taint Analysis
TODO

![Taint Analysis](../images/taint-analysis.png)