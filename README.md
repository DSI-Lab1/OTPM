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
- **TargetUM**: A tree-based algorithm known as Target-based high-Utility iteMset querying using (TargetUM) is proposed. The algorithm uses a lexicographic querying tree and three effective pruning strategies to improve the mining efficiency.
- **TaSPM**: The first work for mining sequential patterns, which is based on a C-MAP structure.
- **TaSRM**: Several pruning strategies and an optimization are introduced to improve the efficiency of mining target sequential rules.
- **TUSQ**: The first work for mining sequential patterns, which is based on a target chain structure.
- **TALENT**: The algorithm based on Nettree structure is designed to mine non-overlapping sequences. Two search methods including breadth-first and depth-first searching are proposed to troubleshoot the generation of patterns.
- **TaRP**: The first work for mining rare high-utility patterns, which is based on a modified utility-list structure.
- **THUIM**: The improved work for mining high-utility itemsets, with the better efficiency than TargetUM.
- **TMKU**: The work for top-k mining high-utility itemsets.

## Citation
If these articles or codes useful for your project, please refer to
```xml
TargetUM:
@article{miao2022targetum,
	title={Targeted high-utility itemset querying},
	author={Miao, Jinbao and Wan, Shicheng and Gan, Wensheng and Sun, Jiayi and Chen, Jiahui},
	journal={IEEE Transactions on Artificial Intelligence}, 
	volume={},  
	number={},  
	pages={1-13},
	year={2022},
	publisher={IEEE}
}
```


## Notes
If there are any questions, please contact me (osjbmiao@gmail.com).
