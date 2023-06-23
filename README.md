# OTPM
As an important sub-branch of data mining, pattern mining is nowadays applied in various fields such as healthcare, finance, traffic, and shopping basket. In order to obtain useful and concise results of interest to the user, the targeted mining method imposes a query constraint that avoids the generation of irrelevant patterns. So far, researchers have proposed many targeted mining algorithms for different mining tasks. In order to provide relevant researchers with convenient use and learning of targeted pattern mining, a project called OTPM (Open-source of Targeted Pattern Mining) deployed on GitHub is developed, which contains some targeted mining algorithms in recent years.

## Requirements
Java programming language.

## Running the program
A simple way is to run the java test file.

## Benchmarks
- Sequence data
- Transaction data
- Biological sequence

## Introduction
- **TargetUM**: A tree-based algorithm known as Target-based high-Utility iteMset querying using is proposed. The algorithm uses a lexicographic querying tree and three effective pruning strategies to improve the mining efficiency.
- **TaSPM**:  A generic algorithm namely TaSPM, based on the fast CM-SPAM algorithm. The idea of it is based on the bitmap comparison.
- **TaSRM**: Several pruning strategies and an optimization are introduced to improve the efficiency of mining target sequential rules.
- **TUSQ**: The algorithm is based on two novel upper bounds (suffix remain utility and terminated descendants utility) as well as a vertical last instance table. For further efficiency, TUSQ relies on a projection technology utilizing a compact data structure called the targeted chain. 
- **TALENT**: The algorithm based on Nettree structure is designed to mine non-overlapping sequences. Two search methods including breadth-first and depth-first searching are proposed to troubleshoot the generation of patterns.
- **TaRP**: The first work for mining rare target high-utility patterns, which is based on a modified utility-list structure.
- **THUIM**: The improved work for mining target high-utility itemsets, with the better efficiency than TargetUM.
- **ITUS**: The work for mining target high-utility seuqneces from dynamic data.

## Citation
If these papers or code are useful for your project, please cite the relevant papers.

## Notes
If there are any questions, please contact us.
