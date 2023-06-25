# OTPM
As an important sub-branch of data mining, pattern mining is nowadays applied in various fields such as healthcare, finance, traffic, and shopping basket. In order to obtain useful and concise results of interest to the user, the targeted mining method imposes a query constraint that avoids the generation of irrelevant patterns. So far, researchers have proposed many targeted mining algorithms for different mining tasks. In order to provide relevant researchers with convenient use and learning of targeted pattern mining, a project called OTPM (Open-source of Targeted Pattern Mining) deployed on GitHub is developed, which contains some targeted mining algorithms in recent years.

## Requirements
Java programming language.

## Running the program
- A simple way is to run java test files using Java development tools (e.g., IntelliJ IDEA and Eclipse).
- You can also run the jar package by executing the Java Command Line.

## Benchmarks
- Sequence data
- Transaction data
- Biological sequence
  
Other types of data will be supported in future work.

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
```xml
@inproceedings{miao2021targeted,
  title={Targeted high-utility itemset querying},
  author={Miao, Jinbao and Wan, Shicheng and Gan, Wensheng and Sun, Jiayi and Chen, Jiahui},
  booktitle={The IEEE International Conference on Big Data},
  pages={5534--5543},
  year={2021},
  organization={IEEE}
}

@article{zhang2022tusq,
  title={{TUSQ}: Targeted high-utility sequence querying},
  author={Zhang, Chunkai and Dai, Quanjian and Du, Zilin and Gan, Wensheng and Weng, Jian and Yu, Philip S.},
  journal={IEEE Transactions on Big Data},
  volume={9},
  number={2},
  pages={512--527},
  year={2022},
  publisher={IEEE}
}

## Notes
If there are any questions, please contact us.
