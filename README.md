# MarioPCG
## SECTION 1 : PROJECT TITLE
<div align="center">
  <img src="https://zekunsgames.oss-us-west-1.aliyuncs.com/marioUI.png" alt="Project Title">
</div>

---

## SECTION 2 : EXECUTIVE SUMMARY / REPORT ABSTRACT

Our system is a game development system that integrates "automatic generation", "automatic testing" and "level selection". It is committed to solving the intelligent map design of the game industry. It uses a Procedural Content Generation (PCG) technology, which is based on Generative Adversarial Networks (GANs), Covariance Matrix Adaptation Evolution Strategy (CMA-ES), etc.

The specific game in this project is Super Mario Bros, but the technique should generalize to any game for which an existing corpus of levels is available. Our GAN is trained on a single level from the original Super Mario Bros, available as part of the Video Game Level Corpus (VGLC). CMA-ES is then used to find ideal inputs to the GAN from within its latent vector space. During the evolution, the generated levels are evaluated using different fitness functions. This allows for the discovery of levels that exist between and beyond those sparse examples designed by human designers, and that also optimize additional goals. Our approach is capable of generating playable levels that meet various goals and is ready to be applied to level generation of other games, such as the games in the GVGAI framework. By training on only a single level, we are able to show that even with a very limited dataset, we can apply the presented approach successfully.

The main function of our system is to be able to generate Mario game maps, and to provide automatic detection and trial play of new maps. This function is mainly realized through GAN & CMA-ES and other technologies. And our UI interface is designed based on java-swing. In the Techniques/Algorithms chapter of the project report, we discussed these technologies and algorithms in detail.

---

## SECTION 3 : CREDITS / PROJECT CONTRIBUTION

| Official Full Name  | Student ID (MTech Applicable)  | Work Items (Who Did What) | Email (Optional) |
| :------------ |:---------------:| :-----| :-----|
| Tao Xiyan | A0215472J | todo:contribution | e0535562@u.nus.edu |
| Xiao Yuchao | A0215486Y | todo:contribution | yuchao_xiao@u.nus.edu |
| Zhang Zekun | A0215485B | todo:contribution | zhang_zekun@u.nus.edu |

---

## SECTION 4 : VIDEO OF SYSTEM MODELLING & USE CASE DEMO



---

## SECTION 5 : USER GUIDE

`Refer to appendix User Guide in Doc Folder: ` <a href="https://github.com/ISA-has-two-projcet/MarioPCG/blob/master/Doc/MarioAiPCGUserGuide.pdf">click here</a>

---

## SECTION 6 : PROJECT REPORT / PAPER

---



