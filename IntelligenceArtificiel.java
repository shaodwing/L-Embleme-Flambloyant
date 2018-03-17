import java.util.ArrayList;
import java.util.Iterator;

public class IntelligenceArtificiel { 
	private static boolean[] peutAttaquerIndiceJoueur = {false, false, false};
	protected static ArrayList<int[]> caseJouable = new ArrayList<int[]>();
	private static Joueur joueur = Board.joueur;
	private static Ennemi ennemi = Board.ennemi;
	public static boolean personnageEnnemiPeutAttaquer = false;
	public static int personnageJoueurCible = -1;
	
	public static void activerIntelligenceArtificiel(Personnage perso, Joueur joueurTemp, Ennemi ennemiTemp) {
		boolean peutAttaquer = false, peutSeDeplacer;
		joueur = joueurTemp;
		ennemi = ennemiTemp;
		//On efface les ancienne donnees de l'IA
		reinitialisationIA();
		
		//On recherche les actions possible par l'IA
		//parcoursCarte() rempli le tableau peutAttaquerIndiceJoueur
		parcoursCarte(perso);
		for (int i = 0; i < peutAttaquerIndiceJoueur.length; i++) {
			if(peutAttaquerIndiceJoueur[i]) {
				peutAttaquer = true;
				System.out.println("Peut attaquer joueur "+ i);
			}
		}
		
		if(!caseJouable.isEmpty()) peutSeDeplacer = true;
		else peutSeDeplacer = false;
		
		
		/*
		 * On selectionne l'action utiliser sachant que:
		 * - L'attaque a la prioriter sur tout
		 * - Si le personnage ne peut pas faire d'action, alors il passe son tour
		 */
		
		if(peutAttaquer || peutSeDeplacer) {
		//Recherche de la meilleur cible
			boolean[] peutTuer = {false, false, false};
			double[] degat = new double[3];
			int[] pointDeLattaque = new int[3];
			/*
			 * Simulation d'attaque sur chaque joueur attaquable
			 * Selection de la meilleur attaque sachant qu'une attaque qui tue son adversaire est prioritaire aux autres
			 */
			
			for (int i = 0; i < degat.length; i++) {
				if(joueur.getPersonnages(i) != null) {
					degat[i] = perso.getDegat(joueur.getPersonnages(i));
					if(joueur.getPersonnages(i).getPointsDeVie() - degat[i] <= 0) {
					//Le personnage cible sera tue
						peutTuer[i] = true;
					}
				}
				else degat[i] = -1; //Les degat negatif seront toujours inferieur aux vrai degats
			}
			
			//Calcul des points de l'attaque
			for (int i = 0; i < pointDeLattaque.length; i++) {
				pointDeLattaque[i] = peutTuer[i] ? 200 : 0;
				pointDeLattaque[i] += (int)(degat[i]);
			}
			
			//Selection de l'attaque
			int meilleurindice = 0;
			for (int i = 0; i < pointDeLattaque.length; i++) {
				if( pointDeLattaque[meilleurindice] < pointDeLattaque[i]) {
					meilleurindice = i;
				}
			}
			
			//Deplacement du personnage
			Personnage cible = joueur.getPersonnages(meilleurindice);
			int xRecherche = cible.getCaseX();
			int yRecherche = cible.getCaseY();
			if(peutAttaquer) {
				//Attaque
					//Recherche de la case ou le personnage ira pour attaquer
					System.out.println("Position joueur : " + xRecherche + " " + yRecherche);
					int xTest, yTest, i = 0;
					boolean caseAdversaireTrouve = false;
					while (i < caseJouable.size() && !caseAdversaireTrouve) {
						xTest = caseJouable.get(i)[0];
						yTest = caseJouable.get(i)[1];
						if(Math.pow(yRecherche - yTest, 2) + Math.pow(xRecherche - xTest, 2) <= 1) {
						// La case est a cote de la cible, on peut donc attaquer la cible
							caseAdversaireTrouve = true;
							System.out.println("Case pour attaquer trouve : " + xTest + " " + yTest);
							if( !(xTest == perso.getCaseX() && yTest == perso.getCaseY()) ) {
							//Si le personnage a besoin de se deplacer
								perso.deplacer(xTest, yTest);
								personnageEnnemiPeutAttaquer = true;
								personnageJoueurCible = meilleurindice;
							}
							else {
								perso.attaque(cible);
							}
						}	
						i++;
					}
				}
				else if(peutSeDeplacer) {
				//Deplacement
					int indiceCaseLaPlusProche = 0;
					for (int i = 0; i < caseJouable.size(); i++) {
						if(Methode.distance(xRecherche, yRecherche, caseJouable.get(i)[0], caseJouable.get(i)[1]) < Methode.distance(xRecherche, yRecherche, caseJouable.get(indiceCaseLaPlusProche)[0], caseJouable.get(indiceCaseLaPlusProche)[1])) {
							indiceCaseLaPlusProche = i;
						}
					}
					
					perso.deplacer(caseJouable.get(indiceCaseLaPlusProche)[0], caseJouable.get(indiceCaseLaPlusProche)[1]);
				}
		}
		
		
		
		perso.terminerTour();
	}

	
	private static void reinitialisationIA() {
		for (int i = 0; i < peutAttaquerIndiceJoueur.length; i++) {
			peutAttaquerIndiceJoueur[i] = false;
		}
		
		personnageAAttaquer();
		caseJouable.clear();
		System.out.println("IA reini");
	}

	public static void personnageAAttaquer() {
	//Reinitialise quelques qttribut apres l'attaque
		personnageEnnemiPeutAttaquer = false;
		personnageJoueurCible = -1;
	}
	
	public static void parcoursCarte(Personnage perso) {
		int[][] carte = Carte.getCarte();
		int profondeur = 3;
		
		//TEST
		System.out.println(joueur.getPersonnages(0).getCaseX() + " " + joueur.getPersonnages(0).getCaseY());
		afficherCarteTerminal(carte);
		//FIN TEST
		
		int x = perso.getCaseX();
		int y = perso.getCaseY();
		int[] intTemp = {x,y};
		caseJouable.add(intTemp);
		carte[y][x] = 200;
		parcoursCarteRecursif(x - 1, y, profondeur, carte);
		parcoursCarteRecursif(x + 1, y, profondeur, carte);
		parcoursCarteRecursif(x, y + 1, profondeur, carte);
		parcoursCarteRecursif(x, y - 1, profondeur, carte);

		//TEST
		afficherCarteTerminal(carte);
		//FIN TEST
	}
	
	
	protected static void parcoursCarteRecursif(int x, int y, int profondeur, int[][] carte) {
		if(x >= 0 && y >= 0 && x < carte[0].length && y < carte.length) {
			int indicePersonnageEnnemi = joueur.selectionIndicePersonnage(x, y);
			if(indicePersonnageEnnemi > -1) {
			//Un ennemi est trouve
				peutAttaquerIndiceJoueur[indicePersonnageEnnemi] = true;
			}
			else {
			//La case est vide, on avance
				int profondeurConsomee = 0;
				switch(carte[y][x]){
					case 3: //case normal
						profondeurConsomee = 1;
						break;
					case 4: //Case d'arbre
						profondeurConsomee = 2;
						//Les arbres consomme deux point de mouvements
						break;
					case 10: //Case deja valide
						profondeurConsomee = 100;
						break;
					case 0: //case vide
						profondeurConsomee = 100;
						break;
					default:
						profondeurConsomee = 100;
				}
	
				if(profondeurConsomee == 0 )System.out.println("Case inconnue");
				profondeur -= profondeurConsomee;
				if(profondeur > 0) {
				//Si on a assez de point pour aller dans la case
					carte[y][x] = 10; //On valide la case
					
					int[] temptab = {x, y};
					if(!ennemi.caseEstRempliParPersonnage(x, y)) {
					//Si un personnage allie est sur la case, on ne l'ajoute pas aux caseJouable
						caseJouable.add(temptab);
					}
					parcoursCarteRecursif(x - 1, y, profondeur, carte);
					parcoursCarteRecursif(x + 1, y, profondeur, carte);
					parcoursCarteRecursif(x, y + 1, profondeur, carte);
					parcoursCarteRecursif(x, y - 1, profondeur, carte);
				}
			}
		}
	}
	
	public static void afficherCarteTerminal(int[][] carte) {
		for (int j = 0; j < carte.length; j++) {
			for (int i = 0; i < carte[0].length; i++) {
				System.out.print(carte[j][i] + " ");
			}
			System.out.println();
		}
	}
}