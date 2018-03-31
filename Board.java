import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JPanel;
import javax.swing.Timer;

/*
 * Ecrire l'IA:
 * selection de la case pour aller attaque
 * deplacer le personnage
 * finir le tour
 * 
 * faire les autres cas dans l'IA
 */


@SuppressWarnings("serial")
public class Board extends JPanel implements ActionListener, MouseListener {
	//Attribut relative a l'affichage
	public static int tailleCaractereY = (int)(6*Application.SCALE);
	public static int tailleCaractereX = (int)(tailleCaractereY*0.6);
	public static final String racine = "Sprite/";

	//Attribut relative au element de jeu
	private static Carte carte;
	protected static Joueur joueur;
	protected static Ennemi ennemi = new Ennemi();
	private static boolean attendDeselectionOuAttaque = false, partieGagner, partieFinie = false;
	public static boolean personnageSelectionner = false,animationEnCours = false, tourEnnemi = false;
	private boolean enJeu = false;
	public static int indicePersonnageSelectionner = -1;

	//Attribut relative au temps
	private Timer timer; // Sert à actualiser les positions des joueurs et ennemis
	private final static int IMAGE_PAR_SECONDE_VOULU = 60; // Nombre d'image par seconde souhaite (60 = bonne qualite)
	private final static int DELAY_IMAGE = 1000 / IMAGE_PAR_SECONDE_VOULU; // Temps entre deux d'image (en ms)
	private final static int DELAY_UPDATE = 300; // Temps entre deux actualisation (en ms)
	private double tempsTemp = System.currentTimeMillis();
	private int nombreImageParSeconde = 0;
	private static int imagePasseSansUpdate = 0;
	
	
	public Board() {
		addKeyListener(new TAdapter()); //Active l'écoute des touches du clavier
		
		this.addMouseListener(this);
		
		setFocusable(true); //Permet de pouvoir mettre la fenêtre en premier-plan 
		setBackground(new Color(210, 180, 140));
		timer = new Timer(DELAY_IMAGE,this); 
		timer.start(); //Le timer démarre ici

		//Initialise mes variables
		chargerClasse();
		carte = new Carte();
		creationJoueur();
		creationEnnemi(3);
		Carte.enleverCaseApparition();
		Case.genererCarte(joueur, ennemi, -1);
	}
	
	
	private static void creationJoueur() {
    	Scanner sc = new Scanner(System.in);
    	System.out.println("Entrez votre nom.");
    	String nom = sc.nextLine();
    	Board.joueur = new Joueur(nom);

    	System.out.println("Voulez-vous une equipe equilibre preparee ? (y/n)"
    			+ "\n(Un guerrier de chaque classe)");
    	char choix = sc.nextLine().charAt(0);
    	if(choix == 'y') {
    		Board.joueur.ajouterPersonnage(new Hache(true));
    		Board.joueur.ajouterPersonnage(new Epee(true));
    		Board.joueur.ajouterPersonnage(new Lance(true));
    	}
    	else {
    		int nombreGuerrierChoisi = 0, choixGuerrier;
    		String classe = "";
			System.out.println("Choississez vos trois guerriers parmis les trois classes suivante :");
    		while(nombreGuerrierChoisi < 3) {
    			System.out.println("-1 : Epeiste (Guerrier equilibre)\n"
	    			+ "-2 : Barbare (Guerrier ayant une plus grande defence)\n"
	    			+ "-3 : Halbardier (Guerrier une plus grande attaque)\n");
    			choixGuerrier = sc.nextInt();
    			
    			if(0 < choixGuerrier && choixGuerrier < 4) {
        			nombreGuerrierChoisi++;
    				switch(choixGuerrier) {
    				case 1:
    		    		Board.joueur.ajouterPersonnage(new Epee(true));
    		    		classe = "Epeiste";
    					break;
    				case 2:
    		    		Board.joueur.ajouterPersonnage(new Hache(true));
    		    		classe = "Barbare";
    					break;
    				case 3:
    		    		Board.joueur.ajouterPersonnage(new Lance(true));
    		    		classe = "Halbardier";
    					break;
    				}
    				System.out.println(classe + " ajoute avec succes.");
    			}
    			else {
    				System.out.println("Entrez clavier non comprise, veillez ressayer.");
    			}
    		}
    	}
    	sc.close();
    }
	
	private static void creationEnnemi(int nbPersonnage) {
		Personnage persoTemp = null;
		for (int i = 0; i < nbPersonnage; i++) {
			switch(Methode.nombreAlea(1, 3)) {
			case 1:
				persoTemp = new Epee(false);
				break;
			case 2:
				persoTemp = new Hache(false);
				break;
			case 3:
				persoTemp = new Lance(false);
				break;
			}
			
			ennemi.ajouterPersonnage(persoTemp);
		}
	}

	
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		doDrawing(g);
		Toolkit.getDefaultToolkit().sync(); //Nécessaire au fonctionnement de swing
		
		//Calcul et affiche le nombre d'image par seconde (IPS)
		this.nombreImageParSeconde++;
		if(System.currentTimeMillis() - this.tempsTemp > 1000) {
		//Chaque seconde, on met a jour les IPS
			Application.ex.setTitle(Application.titreFenetre + " | IPS: " + this.nombreImageParSeconde);
			this.tempsTemp = System.currentTimeMillis();
			this.nombreImageParSeconde = 0;
		}
	}
	

	//méthode appelée pour mettre à jour l'affichage
	private void doDrawing(Graphics g){ 
		Graphics2D g2d = (Graphics2D) g; //On cast g en graphics2D(bibliothèque Java) pour utiliser la méthode drawImage()
		carte.dessiner(this, g2d);
		(new Case()).dessiner(this, g2d);
		joueur.dessiner(this, g2d);
		ennemi.dessiner(this, g2d);
		dessinerInformation(this, g2d);
		
		if(animationEnCours && personnageSelectionner) {
		//Si il y a une animation du joueur, on le redessine
			joueur.getPersonnages(indicePersonnageSelectionner).dessiner(this, g2d);
		}
		
		if(partieFinie) {
			dessinerFinPartie(this, g2d);
		}
		
		this.enJeu = true;
	}
	

	private static void dessinerInformation(Board board, Graphics2D g2d) {
		int offsetYEnPixel = Carte.getHauteurEnPixel() + tailleCaractereY; //La fin de la carte
		int offsetXEnCase = 0;
		g2d.setFont(new Font("Monospaced", Font.PLAIN, tailleCaractereY));
		g2d.setColor(Color.BLACK);
		
		offsetXEnCase = joueur.dessinerInformation(board, g2d, offsetXEnCase, offsetYEnPixel);
		offsetXEnCase += 5;
		offsetXEnCase = ennemi.dessinerInformation(board, g2d, offsetXEnCase, offsetYEnPixel);
	}
	
	private static void dessinerFinPartie(Board board, Graphics2D g2d) {
		int longueurMax = 0;
		int x, y, longueur, largeur,  epaisseurBord, posX, posY;
		double espace;
		ArrayList<String> messageCoupe = new ArrayList<String>();
		if(partieGagner) {
			messageCoupe.add("Bravo " + joueur.getNom() + ", vous avez");
			messageCoupe.add("vaincu l'equipe ennemie et");
			messageCoupe.add("recuperer l'embleme flamboyant !");
		}
		else {
			messageCoupe.add("L'equipe ennemie s'empare");
			messageCoupe.add("de l'embleme flamboyant !");
			messageCoupe.add("Vous ferrez mieux la");
			messageCoupe.add("prochaine fois.");
		}
		
		//Dessine le carre aui sert de fond
		for (String message : messageCoupe) {			
			if(message.length() > longueurMax) {
				longueurMax = message.length();
			}
		}
		
		posX = 20;
		posY = 5;
		epaisseurBord = 5;
		espace = 0.5;
		x = (int) (Board.tailleCaractereX * (posX-espace));
		y = (int) (Board.tailleCaractereY * posY);
		longueur = (int) (Board.tailleCaractereX * (longueurMax + 3 + 2 * espace));
		largeur = (int) (Board.tailleCaractereY * (messageCoupe.size() + 0.4));

		g2d.setColor(Color.BLACK);
		g2d.fillRect(x-epaisseurBord, y-epaisseurBord, longueur+2*epaisseurBord, largeur+2*epaisseurBord);
		g2d.setColor(Color.WHITE);
		g2d.fillRect(x, y, longueur, largeur);

		
		//Affichage du texte
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("Monospaced", Font.PLAIN, tailleCaractereY));
		
		int i = 0;
		for (String message : messageCoupe) {
			g2d.drawString(message, Board.tailleCaractereX * posX, Board.tailleCaractereY * ( i + posY +1 ) );
			i++;
		}
		

		
		/*			g2d.drawString(informationListe[i], (int)(offsetXEnCase*Board.tailleCaractereX),
												offsetYEnPixel+Board.tailleCaractereY*i);*/
	}


	public void actionPerformed(ActionEvent e){
	//Mise à jour periodique des positions et index d'animation des entités mouvantes
		if(enJeu) {
			miseAJourDesIndicesDImage();
			//Action de jeu
			if(!finPartie()) {
				echangerTour();
				if(tourEnnemi) {
					ennemi.tourEnnemi();
				}
			}
		}
		/*else if(finPartie()) {
			if(partieGagner) {
			//Le joueur a gagner
				
			}
			else if(partiePerdu) {
			//L'ennemi a gagne
				
			}
		}
		
		*/

		repaint(); //Affiche l'image
	}
	

	private boolean finPartie() {
		boolean partieFinie = false;
		if(!animationEnCours) {
			if(joueur.getPersonnages().size() == 0) {
				ennemi.victoire();
				partieFinie = true;
				partieGagner = false;
			}
			if(ennemi.getPersonnages().size() == 0) {
				joueur.victoire();
				partieFinie = true;
				partieGagner = true;
			}
			
			if(partieFinie) {
				Board.partieFinie = true;
				Case.genererCarte(joueur, ennemi, indicePersonnageSelectionner);
				repaint();
				//System.out.println("La partie est maintenant termine");
				//timer.stop();
			}
		}
		return partieFinie;
	}

	private class TAdapter extends KeyAdapter{ // Méthode qui s'active quand l'état d'une touche change
		@Override
		public void keyReleased(KeyEvent e){ //Action quand une touche est relachee
			if(e.getKeyCode()==KeyEvent.VK_SPACE) { // Si le joueur tape sur la barre espace
				if(!tourEnnemi) {
					joueur.passerTour();
				}
			}
		}
		@Override
		public void keyPressed(KeyEvent e){ //Action quand une touche est pressee
		}
	}

	//Methode qui charge les attribut static
	private static void chargerClasse() {
		Epee.chargerClasse();
		Lance.chargerClasse();
		Hache.chargerClasse();
	}

/*
 * Gestion de la souris et de l'avancement de la partie
 */
	public void mouseClicked(MouseEvent e) { //Evenement quand il y a un click
	    int x=(int)(e.getX()/(Application.SCALE * 16)); //16 est la taille en pixel dune case avec un SCALE de 1
	    int y=(int)(e.getY()/(Application.SCALE * 16));	    

    	int caseCibleIndiceJoueur = joueur.selectionPersonnageJouable(x, y);
    	int caseCibleIndiceEnnemi = ennemi.selectionIndicePersonnage(x, y);
    	if(!tourEnnemi && !animationEnCours && enJeu) {
	    //Les cliques durant le tour ennemi n'ont aucuns effets
	    	if(caseCibleIndiceJoueur > -1) {
	    	//Le joueur selectionne un personnage
	    		
	    		if(indicePersonnageSelectionner == caseCibleIndiceJoueur) {
	    		//Le joueur veut terminer le tour d'un personnage
	    			joueur.getPersonnages(caseCibleIndiceJoueur).terminerTour();
	    			deselectionnePersonnage();
	    		}
	    		else {
	    		//Le joueur veut selectionner un nouveau personnage
	    			if(personnageSelectionner) deselectionnePersonnage();
		    		selectionnePersonnage(caseCibleIndiceJoueur);
	    		}
	    	}
	    	else if(personnageSelectionner && indicePersonnageSelectionner > -1) {
	    	//Le joueur a deja selectionner un personnage et souhaite l'utiliser
	    		if(caseCibleIndiceEnnemi > -1 && personnageSelectionnerPeutAttaquer(caseCibleIndiceEnnemi)) {
	    		//Le joueur cible et peut attaquer un ennemi
    				joueur.attaquePersonnage(indicePersonnageSelectionner, caseCibleIndiceEnnemi, ennemi);
    				attendDeselectionOuAttaque = false;
	    		}
	    		else if(Case.estCaseValidePourDeplacement(x, y) && caseCibleIndiceEnnemi == -1 && caseCibleIndiceJoueur == -1) {
	    		//Le joueur deplace son personnage <=> ne vise pas d'ennemi ni un joueur mais une case jouable
	    		//Les Personnages sont traversables
	    			Carte.deplacerPersonnage(x, y, joueur, indicePersonnageSelectionner);
	    			joueur.getPersonnages(indicePersonnageSelectionner).terminerTour();
	    			//Le personnage est deselectionner via la fonction d'animation
	    		}
	    		else if(caseCibleIndiceEnnemi == -1 && caseCibleIndiceJoueur == -1) {
	    		//Le joueur Deselectionne son personnage en visant une case vide
	    			deselectionnePersonnage();
	    		}
	    	}
	    }
	}
	
	
	public static void personnagePeutAttaquerApresDeplacement() {
	//Methode appele apres le deplacement (A la fin de l'animation)
		if(personnageSelectionnerPeutAttaquer()) {
			attendDeselectionOuAttaque = true;
			joueur.getPersonnages(indicePersonnageSelectionner).peutAttaquerApresDeplacement();
		}
		else {
			deselectionnePersonnage();
		}
	}
	
	
	public static boolean personnageSelectionnerPeutAttaquer() {
		return joueur.getPersonnages(indicePersonnageSelectionner).peutAttaquer(ennemi);
	}
	
	public static boolean personnageSelectionnerPeutAttaquer(int indice){
		boolean peutAttaquer = false;
		 if(indice >= 0)
			 peutAttaquer = joueur.getPersonnages(indicePersonnageSelectionner).
									peutAttaquer(ennemi.getPersonnages(indice));
		
		return peutAttaquer;
	}
	

	public static void deselectionnePersonnage() {
		joueur.getPersonnages(indicePersonnageSelectionner).deselectionner();
		indicePersonnageSelectionner = -1;
		personnageSelectionner = false;
		attendDeselectionOuAttaque = false;
		Case.genererCarte(joueur, ennemi, indicePersonnageSelectionner);
	}
	
	
	public static void selectionnePersonnage(int indicePersoSelectionner) {
		personnageSelectionner = true;
		attendDeselectionOuAttaque = false;
		indicePersonnageSelectionner = indicePersoSelectionner;
		Case.genererCarte(joueur, ennemi, indicePersonnageSelectionner);
	}
	
	
	private static void echangerTour(){
		if(!animationEnCours && !attendDeselectionOuAttaque) {
		//Echange les tour des intelligences
			if(joueur.ATerminerSonTour() && !tourEnnemi) {
				tourEnnemi = true;
				ennemi.debutTour();
				Case.genererCarte(joueur, ennemi, -1);
			}
			else if(ennemi.ATerminerSonTour() && tourEnnemi) {
				tourEnnemi = false;
				joueur.debutTour();
				Case.genererCarte(joueur, ennemi, -1);
			}
		}
	}
	
	
	public static void miseAJourDesIndicesDImage() {
		imagePasseSansUpdate++;
		if(DELAY_UPDATE - DELAY_IMAGE*imagePasseSansUpdate  < 0){
			ennemi.update();
			joueur.update();
			imagePasseSansUpdate = 0;
		}
	}
	
	
/*
 * Methode implementer du listener de la souris
 */
	//Le listener de la souris etant un template, on doit override les methode abstract meme si elles sont vides.
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}

	/**
	 * @return the carte
	 */
	public Carte getCarte() {
		return carte;
	}
}



